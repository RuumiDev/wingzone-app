import qz from 'qz-tray';
import { KJUR, hextob64 } from 'jsrsasign';

interface ThermalPrintConfig {
  printerName?: string;
  paperWidth?: number; // in mm, default 80mm
}

export class ThermalPrinterService {
  private static instance: ThermalPrinterService;
  private connected: boolean = false;

  private constructor() {}

  static getInstance(): ThermalPrinterService {
    if (!ThermalPrinterService.instance) {
      ThermalPrinterService.instance = new ThermalPrinterService();
    }
    return ThermalPrinterService.instance;
  }

  async connect(): Promise<boolean> {
    try {
      if (!qz.websocket.isActive()) {
        // Load certificate from public folder
        qz.security.setCertificatePromise(function(resolve: any, reject: any) {
          fetch("/certs/digital-certificate.txt")
            .then(response => response.text())
            .then(resolve)
            .catch(reject);
        });

        // Load private key and set up signing with SHA512
        qz.security.setSignatureAlgorithm("SHA512");
        qz.security.setSignaturePromise(function(toSign: string) {
          return function(resolve: any, reject: any) {
            fetch("/certs/private-key.pem")
              .then(response => response.text())
              .then(privateKey => {
                try {
                  // Create signature using jsrsasign
                  const sig = new KJUR.crypto.Signature({ alg: "SHA512withRSA" });
                  sig.init(privateKey);
                  sig.updateString(toSign);
                  const signature = hextob64(sig.sign());
                  resolve(signature);
                } catch (error) {
                  console.error('[QZ Tray] Signing error:', error);
                  reject(error);
                }
              })
              .catch(reject);
          };
        });
        
        console.log('[QZ Tray] Connecting with certificate authentication...');
        await qz.websocket.connect();
      }
      this.connected = true;
      console.log('[QZ Tray] ✓ Connection established');
      return true;
    } catch (error) {
      console.error('[QZ Tray] ✗ Connection failed:', error);
      this.connected = false;
      return false;
    }
  }

  async disconnect(): Promise<void> {
    try {
      if (qz.websocket.isActive()) {
        await qz.websocket.disconnect();
      }
      this.connected = false;
    } catch (error) {
      console.error('QZ Tray disconnect failed:', error);
    }
  }

  async getPrinters(): Promise<string[]> {
    try {
      if (!this.connected) {
        await this.connect();
      }
      return await qz.printers.find();
    } catch (error) {
      console.error('Failed to get printers:', error);
      return [];
    }
  }

  async printReceipt(order: any, config: ThermalPrintConfig = {}, memberData?: { name: string; cartItems: any[]; index: number; total: number }): Promise<boolean> {
    try {
      const receiptType = memberData ? `individual member (${memberData.name})` : 'master';
      console.log(`[QZ Tray] Starting ${receiptType} print job...`);
      console.log('[QZ Tray] Order data:', {
        location: order.location,
        selectedLocation: order.selectedLocation,
        paymentMethod: order.paymentMethod,
        lobbyPaymentMethod: order.lobbyPaymentMethod,
        isGroupOrder: order.isGroupOrder,
        lobbyId: order.lobbyId,
        hasMembers: !!order.members,
        memberCount: order.members?.length
      });
      
      if (!this.connected) {
        console.log('[QZ Tray] Not connected, attempting connection...');
        const connected = await this.connect();
        if (!connected) {
          console.error('[QZ Tray] Connection failed');
          throw new Error('Could not connect to QZ Tray');
        }
        console.log('[QZ Tray] Connected successfully');
      }

      // Get printer name
      let printerName = config.printerName;
      if (!printerName) {
        console.log('[QZ Tray] No printer specified, detecting printers...');
        const printers = await this.getPrinters();
        console.log('[QZ Tray] Found printers:', printers);
        
        if (printers.length === 0) {
          console.error('[QZ Tray] No printers found');
          throw new Error('No printers found');
        }
        // Use first printer or find thermal printer
        printerName = printers.find(p => 
          p.toLowerCase().includes('thermal') || 
          p.toLowerCase().includes('pos') ||
          p.toLowerCase().includes('receipt')
        ) || printers[0];
        console.log('[QZ Tray] Auto-selected printer:', printerName);
      } else {
        console.log('[QZ Tray] Using configured printer:', printerName);
      }

      // Generate ESC/POS commands
      console.log('[QZ Tray] Generating receipt commands...');
      const commands = this.generateReceiptCommands(order, config.paperWidth || 80, memberData);
      console.log('[QZ Tray] Generated', commands.length, 'commands');

      // Configure print job
      const printConfig = qz.configs.create(printerName);

      // Print receipt (no separate logo job to avoid paper cut)
      console.log('[QZ Tray] Printing receipt...');
      await qz.print(printConfig, commands);
      console.log('[QZ Tray] ✓ Print job completed successfully');
      return true;
    } catch (error) {
      console.error('[QZ Tray] ✗ Print failed:', error);
      if (error instanceof Error) {
        console.error('[QZ Tray] Error message:', error.message);
        console.error('[QZ Tray] Error stack:', error.stack);
      }
      return false;
    }
  }

  private generateReceiptCommands(order: any, paperWidth: number, memberData?: { name: string; cartItems: any[]; index: number; total: number }): string[] {
    const commands: string[] = [];
    const ESC = '\x1B';
    const GS = '\x1D';
    
    // Initialize printer
    commands.push(`${ESC}@`); // Initialize
    commands.push(`${ESC}a\x01`); // Center align
    
    // WINGZONE branding with branch address based on location
    // Handle location as either string or object {name, address, etc.}
    let locationName = '';
    if (typeof order.location === 'string') {
      locationName = order.location;
    } else if (order.location && typeof order.location === 'object') {
      locationName = order.location.name || order.location.id || '';
    }
    
    let branchName = '(MERU BRANCH)';
    let branchAddress1 = 'Petronas Lebuh Meru Raya Susuran Meru Raya,';
    let branchAddress2 = 'Bandar Meru Raya 30030 Ipoh, Perak.';
    
    if (locationName.toLowerCase().includes('greentown')) {
      branchName = '(GREENTOWN BRANCH)';
      branchAddress1 = '20 Persiaran Greentown 1,';
      branchAddress2 = 'Greentown Business Center, 30450 Ipoh, Perak';
    }
    
    commands.push('\n');
    commands.push('****************************************\n');
    commands.push(`${ESC}!\x77`); // Quadruple size: 4x width + 4x height + bold
    
    commands.push('W I N G Z O N E\n');
    commands.push(`${ESC}!\x00`); // Reset to normal
    commands.push(`${branchName}\n`);
    commands.push(`${ESC}!\x00`); // Reset to normal
    commands.push(`${branchAddress1}\n`);
    commands.push(`${branchAddress2}\n`);
    commands.push('****************************************\n');
  
    
    // Order type banner
    commands.push(`${ESC}!\x30`); // Double size
    
    // Show if it's master or member receipt
    if (memberData) {
      commands.push('MEMBER RECEIPT\n');
      commands.push(`${ESC}!\x10`); // Bold
      commands.push(`${memberData.name.toUpperCase()}\n`);
      commands.push(`${ESC}!\x00`); // Normal
    } else if (order.lobbyId || order.isGroupOrder) {
      commands.push('MASTER RECEIPT\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push('Kitchen Production Copy\n');
    } else {
      // Show order type in double-size
      console.log('[RECEIPT] Order type:', order.orderType, 'Type:', typeof order.orderType);
      const type = (order.orderType || 'pickup').toLowerCase().replace(/[_-]/g, '');
      const orderType = type === 'dinein' ? 'DINE-IN' : 'PICKUP';
      commands.push(orderType + '\n');
      commands.push(`${ESC}!\x00`); // Normal
    }
    
      commands.push('****************************************\n');
    
    // Group order info
    if (order.lobbyId || order.isGroupOrder) {
      commands.push(`LOBBY ID: #${order.code || order.groupOrderCode}\n`);
      
      // Order Type (Pickup/Dine-in)
      const type = (order.orderType || '').toLowerCase().replace(/[_-]/g, '');
      const orderTypeDisplay = type === 'dinein' ? 'DINE-IN' : (type === 'pickup' ? 'PICKUP' : 'N/A');
      commands.push(`${ESC}!\x10`); // Bold
      commands.push(`*** TYPE: ${orderTypeDisplay} ***\n`);
      commands.push(`${ESC}!\x00`); // Normal
      
      // Location
      const locationData = order.location || order.selectedLocation;
      if (locationData) {
        let locationText = 'N/A';
        if (typeof locationData === 'string') {
          locationText = locationData;
        } else if (typeof locationData === 'object') {
          locationText = locationData.name || locationData.label || locationData.displayName || 'N/A';
        }
        commands.push(`LOCATION: ${locationText}\n`);
      }
      
      // Payment Method
      if (order.paymentMethod) {
        const payment = order.paymentMethod === 'host-pays-all' || order.paymentMethod === 'host-pays' ? 'HOST PAYS ALL' : 'INDIVIDUAL PAYMENT';
        commands.push(`PAYMENT: ${payment}\n`);
      }
      
      if (!memberData) {
        commands.push(`HOST: ${order.hostUserName || order.userName}\n`);
        commands.push(`MEMBERS: ${order.memberCount || order.members?.length || 0}\n`);
      }
    } else {
      if (order.tableNumber) {
        commands.push(`TABLE: ${order.tableNumber}\n`);
      }
      if (order.userName && order.orderType === 'carry-out') {
        commands.push(`CUST: ${order.userName}\n`);
      }
    }
    
    commands.push('\n');
    
    // Order info
    commands.push(`${ESC}a\x00`); // Left align
    const date = order.createdAt?.toDate ? order.createdAt.toDate() : new Date(order.createdAt);
    commands.push(`Date: ${date.toLocaleDateString('en-MY')}`);
    commands.push(` Time: ${date.toLocaleTimeString('en-MY', { hour: '2-digit', minute: '2-digit' })}\n`);
    commands.push(`Ord#: ${order.id?.substring(0, 8).toUpperCase()}\n`);
    
    if (order.lobbyId) {
      commands.push('Status: PAID (Online Gateway)\n');
    }
    
    commands.push('\n');
    commands.push('----------------------------------------\n');
    
    // Items header
    if (order.lobbyId && !memberData) {
      commands.push(`${ESC}a\x01`); // Center
      commands.push(`${ESC}!\x10`); // Bold
      commands.push('>> KITCHEN PRODUCTION SUMMARY <<\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push('(Cook these items together)\n');
      commands.push(`${ESC}a\x00`); // Left
      commands.push('----------------------------------------\n');
    } else {
      commands.push(`${ESC}!\x10`); // Bold
      commands.push(this.padColumns(['QTY', 'ITEM', 'PRICE'], [4, 20, 8]));
      commands.push(`${ESC}!\x00`); // Normal
      commands.push('----------------------------------------\n');
    }
    
    // Items - use memberData.cartItems for individual receipts
    const itemsToShow = memberData ? memberData.cartItems : order.items;
    if (itemsToShow) {
      order.items.forEach((item: any) => {
        const qty = item.quantity || 1;
        let name = item.menuItem?.name || item.menuItemName || item.name || 'ITEM';
        const itemPrice = item.menuItem?.price || item.price || 0;
        
        // Add (COMBO) label for zero-price items
        if (itemPrice === 0) {
          name += ' (COMBO)';
        }
        
        const price = (itemPrice * qty).toFixed(2);
        
        commands.push(`${ESC}!\x10`); // Bold
        commands.push(this.padColumns([qty.toString(), name, price], [4, 20, 8]));
        commands.push(`${ESC}!\x00`); // Normal
        
        // Customizations with entrees support
        if (item.customization) {
          // Show entree details if present
          if (item.customization.entrees && item.customization.entrees.length > 0) {
            item.customization.entrees.forEach((entree: any, eIdx: number) => {
              commands.push(`> Entree ${eIdx + 1}: ${entree.name || 'Wings'}\n`);
              if (entree.boneType) {
                commands.push(`  - ${entree.boneType}\n`);
              }
              // Only show flavor if the menu item requires it
              if (entree.flavor && String(entree.flavor).toUpperCase() !== 'NONE' && 
                  item.menuItem?.customizationOptions?.requiresFlavor) {
                commands.push(`  - Flavor: ${entree.flavor}\n`);
              }
              // Only show dipping sauce if menu item requires it
              if (entree.dippingSauce && String(entree.dippingSauce).toUpperCase() !== 'NONE' &&
                  item.menuItem?.customizationOptions?.requiresDippingSauce) {
                commands.push(`  - DIP: ${entree.dippingSauce.toUpperCase()} \n`);
              }
              if (entree.friesExchange) {
                commands.push(`  - Side: ${entree.friesExchange.name}\n\n`);
              }
              if (entree.drink) {
                const drinkName = typeof entree.drink === 'string'
                  ? entree.drink
                  : entree.drink.displayName || entree.drink.name;
                if (drinkName && drinkName.toUpperCase() !== 'NONE') {
                  commands.push(`  - DRINK: ${drinkName}\n`);
                }
              }
            });
          }
          
          // Single item customization
          if (item.customization.boneType) {
            commands.push(`> ${item.customization.boneType}\n`);
          }
          // Only show flavor if menu item requires it
          if (item.customization.flavor && String(item.customization.flavor).toUpperCase() !== 'NONE' &&
              item.menuItem?.customizationOptions?.requiresFlavor) {
            commands.push(`> Flavor: ${item.customization.flavor}\n`);
          }
          // Only show dipping sauce if menu item requires it
          if (item.customization.dippingSauce && String(item.customization.dippingSauce).toUpperCase() !== 'NONE' &&
              item.menuItem?.customizationOptions?.requiresDippingSauce) {
            commands.push(`> DIP: ${item.customization.dippingSauce.toUpperCase()} \n`);
          }
          if (item.customization.sideDish) {
            commands.push(`> Side: ${item.customization.sideDish}\n`);
          }
          if (item.customization.saladType) {
            commands.push(`> Salad: ${item.customization.saladType}\n`);
          }
          if (item.customization.friesExchange) {
            let side = `> Side: ${item.customization.friesExchange.name}`;
            if (item.customization.friesExchange.selectedSize === 'jumbo') {
              side += ' (Jumbo)';
            }
            if (item.customization.friesExchange.selectedFlavor) {
              side += ` - ${item.customization.friesExchange.selectedFlavor}`;
            }
            const price = item.customization.friesExchange.selectedSize === 'jumbo' && 
                         item.customization.friesExchange.jumboPrice !== null 
              ? item.customization.friesExchange.jumboPrice 
              : item.customization.friesExchange.regularPrice;
            if (price > 0) {
              side += ` (+RM ${price.toFixed(2)})`;
            }
            commands.push(side + '\n');
          }
          if (item.customization.drink) {
            const drink = typeof item.customization.drink === 'string' 
              ? item.customization.drink 
              : item.customization.drink.displayName || item.customization.drink.name;
            if (drink && drink.toUpperCase() !== 'NONE') {
              commands.push(`> DRINK: ${drink}\n`);
            }
          }
          if (item.customization.specialInstructions) {
            commands.push(`> ${item.customization.specialInstructions}\n`);
          }
        }
        
        // Add spacing after every item (with or without customization)
        commands.push('\n');
      });
    }
    
    // Kitchen summary for individual orders or individual member receipts
    if ((!order.lobbyId && !order.isGroupOrder && order.items) || memberData) {
      commands.push('========================================\n');
      commands.push(`${ESC}a\x01`); // Center
      commands.push(`${ESC}!\x10`); // Bold
      commands.push('>> SUMMARY <<\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push(`${ESC}a\x00`); // Left
      
      console.log('[SUMMARY DEBUG] Individual/Member receipt summary');
      const itemsForSummary = memberData ? memberData.cartItems : order.items;
      console.log('[SUMMARY DEBUG] Items for summary:', itemsForSummary?.length || 0);
      const summary = this.generateKitchenSummary(itemsForSummary);
      console.log('[SUMMARY DEBUG] Generated summary length:', summary.length);
      commands.push(summary);
      commands.push('========================================\n');
    }
    
    // Group order summary - match browser layout (master receipt only)
    if ((order.lobbyId || order.isGroupOrder) && !memberData) {
      commands.push('========================================\n');
      commands.push(`${ESC}a\x01`); // Center
      commands.push(`${ESC}!\x10`); // Bold
      commands.push('>> SUMMARY <<\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push(`${ESC}a\x00`); // Left
      
      // Collect all items from all members
      const allMemberItems: any[] = [];
      if (order.members && order.members.length > 0) {
        console.log('[SUMMARY DEBUG] Master receipt - Found members:', order.members.length);
        order.members.forEach((member: any) => {
          if (member.cartItems) {
            console.log(`[SUMMARY DEBUG] Member ${member.name}: ${member.cartItems.length} items`);
            allMemberItems.push(...member.cartItems);
          }
        });
      } else {
        console.log('[SUMMARY DEBUG] Master receipt - NO MEMBERS FOUND', {
          hasMembers: !!order.members,
          membersLength: order.members?.length
        });
      }
      
      console.log('[SUMMARY DEBUG] Total items for summary:', allMemberItems.length);
      
      // Use the same categorized summary as individual orders
      const summary = this.generateKitchenSummary(allMemberItems);
      console.log('[SUMMARY DEBUG] Generated summary length:', summary.length);
      commands.push(summary);
      
      commands.push('=========\n');
      // Calculate total items from members
      const totalItems = order.members?.reduce((sum: number, member: any) => {
        return sum + (member.cartItems?.reduce((itemSum: number, item: any) => itemSum + (item.quantity || 1), 0) || 0);
      }, 0) || order.totalItems || 0;
      commands.push(`TOTAL ITEMS: ${totalItems}\n`);
      commands.push(`TOTAL PAID:  RM ${(order.groupTotal || order.total || order.totalAmount || 0).toFixed(2)}\n`);
      commands.push('=========\n');
      
      // Packing distribution list
      commands.push(`${ESC}a\x01`); // Center
      commands.push(`${ESC}!\x10`); // Bold
      commands.push('>> PACKING DISTRIBUTION LIST <<\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push(`${ESC}a\x00`); // Left
      
      if (order.members) {
        order.members.forEach((member: any, index: number) => {
          commands.push(`\n[BOX ${index + 1}] - ${member.name.toUpperCase()}\n`);
          
          member.cartItems?.forEach((item: any) => {
            const itemName = item.menuItem?.name || item.menuItemName || item.name || 'ITEM';
            commands.push(`${ESC}!\x10`); // Bold
            commands.push(`${item.quantity}x ${itemName}\n`);
            commands.push(`${ESC}!\x00`); // Normal
            
            if (item.customization) {
              // Show entree details for combos
              if (item.customization.entrees && item.customization.entrees.length > 0) {
                item.customization.entrees.forEach((entree: any, eIdx: number) => {
                  commands.push(` - Entree ${eIdx + 1}: ${entree.name || 'Wings'}\n`);
                  if (entree.boneType) {
                    commands.push(`    - ${entree.boneType}\n`);
                  }
                  if (entree.flavor) {
                    commands.push(`    - Flavor: ${entree.flavor}\n`);
                  }
                  if (entree.dippingSauce) {
                    commands.push(`    - DIP: ${entree.dippingSauce.toUpperCase()} \n`);
                  }
                  if (entree.friesExchange) {
                    commands.push(`    - Side: ${entree.friesExchange.name}\n\n`);
                  }
                });
              }
              
              // Single item customization
              if (item.customization.boneType) {
                commands.push(` - ${item.customization.boneType}\n`);
              }
              if (item.customization.flavor) {
                commands.push(` - Flavor: ${item.customization.flavor}\n`);
              }
              if (item.customization.dippingSauce) {
                commands.push(` - DIP: ${item.customization.dippingSauce.toUpperCase()}\n`);
              }
              if (item.customization.sideDish) {
                commands.push(` - Side: ${item.customization.sideDish}\n`);
              }
              if (item.customization.friesExchange) {
                commands.push(` - Side: ${item.customization.friesExchange.name}\n`);
              }
              if (item.customization.drink && item.customization.drink.displayName) {
                commands.push(` - DRINK: ${item.customization.drink.displayName}\n`);
              }
            }
            
            // Add spacing after every item (with or without customization)
            commands.push('\n'); 
          });
        });
      }
      
      commands.push('\n========================================\n');
    }
    
    // Totals for individual orders or individual member receipts
    if (!order.lobbyId || memberData) {
      commands.push('\n--------------------------------\n');
      
      if (memberData) {
        // Individual member total
        const memberTotal = memberData.total || memberData.cartItems.reduce((sum: number, item: any) => {
          return sum + ((item.price || 0) * (item.quantity || 1));
        }, 0);
        commands.push(this.padColumns(['MEMBER TOTAL', '', memberTotal.toFixed(2)], [10, 12, 10]));
      } else {
        // Regular individual order
        commands.push(this.padColumns(['SUBTOTAL', '', (order.subtotal || order.totalAmount || 0).toFixed(2)], [10, 12, 10]));
        if (order.tax !== undefined) {
          commands.push(this.padColumns(['SST (6%)', '', order.tax.toFixed(2)], [10, 12, 10]));
        }
      }
      
      commands.push('--------------------------------\n');
      commands.push(`${ESC}!\x20`); // Double width
      
      if (memberData) {
        const memberTotal = memberData.total || memberData.cartItems.reduce((sum: number, item: any) => {
          return sum + ((item.price || 0) * (item.quantity || 1));
        }, 0);
        commands.push(this.padColumns(['TOTAL', '', `RM ${memberTotal.toFixed(2)}`], [6, 8, 10]));
      } else {
        commands.push(this.padColumns(['TOTAL', '', `RM ${(order.total || order.totalAmount || 0).toFixed(2)}`], [6, 8, 10]));
      }
      
      commands.push(`${ESC}!\x00`); // Normal
      commands.push('--------------------------------\n');
      
      if (order.paymentStatus === 'paid' || order.lobbyId) {
        commands.push(`${ESC}a\x01`); // Center
        commands.push(`${ESC}!\x30`); // Double size
        commands.push('*** PAID ***\n');
        commands.push(`${ESC}!\x00`); // Normal
        commands.push(`${ESC}a\x00`); // Left
      }
    }
    
    // Footer
    commands.push('\n'); 
    commands.push(`${ESC}a\x01`); // Center
    
    // Company info at bottom
    commands.push(`${ESC}!\x10`); // Bold
    commands.push('Zenith Certification Sdn Bhd\n');
    commands.push(`${ESC}!\x00`); // Normal
    commands.push('Reg: 199401032195 (317877-X)\n');
    commands.push('Lebuh Meru Raya, Bandar Meru Raya\n');
    commands.push('Ipoh, Perak\n\n');
    
    if (order.lobbyId && order.authCode) {
      commands.push(`AUTH CODE: ${order.authCode}\n`);
    }
    commands.push('Wifi: Wingzone123\n');
    commands.push('IG/FB: Wing Zone Malaysia\n');
    
    // Cut paper
    commands.push('\n\n\n');
    commands.push(`${GS}V\x42\x00`); // Feed and cut
    
    return commands;
  }

  private generateKitchenSummary(items: any[]): string {
    // Helper function to normalize keys to Title Case for consistent aggregation
    const normalizeKey = (key: string): string => {
      let normalized = key
        .replace(/_/g, ' ')  // Replace underscores with spaces
        .replace(/-/g, ' ')  // Replace hyphens with spaces
        .toLowerCase()
        .split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
      
      // Normalize common synonyms
      if (normalized === 'Smiley Fries') normalized = 'Smiley';
      if (normalized === 'Coca Cola') normalized = 'Coke';
      
      return normalized;
    };
    
    const mainItems: Record<string, number> = {};
    const sidesItems: Record<string, number> = {};
    const dippingsItems: Record<string, number> = {};
    const drinksItems: Record<string, number> = {};
    
    console.log('[KITCHEN SUMMARY] Processing items:', items.length);
    
    items.forEach((item: any, index: number) => {
      console.log(`[KITCHEN SUMMARY] Item ${index}:`, {
        name: item.menuItemName || item.name,
        hasMenuItem: !!item.menuItem,
        hasKitchenIngredients: !!item.kitchenIngredients,
        menuItemHasKitchen: !!item.menuItem?.kitchenIngredients,
        hasCustomization: !!item.customization,
        itemKeys: Object.keys(item)
      });
      
      const itemQty = item.quantity || 1;
      const kitchen = item.menuItem?.kitchenIngredients || item.kitchenIngredients;
      const boneType = item.customization?.boneType;
      const friesExchange = item.customization?.friesExchange;
      const saladType = item.customization?.saladType;
      const dippingSauce = item.customization?.dippingSauce;
      const drink = item.customization?.drink;
      const entrees = item.customization?.entrees;
      const itemName = (item.menuItem?.name || item.menuItemName || item.name || '').toLowerCase();
      
      // Skip Ranch or Bleu Cheese items - they are dipping sauces, not food
      if (itemName.includes('ranch') || itemName.includes('bleu') || itemName.includes('blue cheese')) {
        console.log(`[KITCHEN SUMMARY] Skipping dipping sauce item: ${itemName}`);
        return;
      }
      
      console.log(`[KITCHEN SUMMARY] Item ${index} extracted data:`, {
        kitchen: !!kitchen,
        ingredientsCount: kitchen?.ingredients?.length || 0,
        hasEntrees: !!entrees,
        entreesCount: entrees?.length || 0
      });
      
      let friesReplaced = false;
      let saladReplaced = false;
      let isComboItem = false;
      
      // Check if this item has kitchen ingredients from menu database
      const hasKitchenIngredients = kitchen?.ingredients && kitchen.ingredients.length > 0;
      
      // Process items using kitchen ingredients from menu database (preferred method)
      if (hasKitchenIngredients) {
        console.log(`[KITCHEN SUMMARY] Processing "${itemName}" with ${kitchen.ingredients.length} ingredients`);
        
        kitchen.ingredients.forEach((ingredient: any) => {
          console.log(`[KITCHEN SUMMARY] Ingredient:`, ingredient);
          
          const lower = ingredient.type.toLowerCase();
          let key = ingredient.type;
          let qty = ingredient.quantity * itemQty;
          
          // FIRST: Strip ALL flavors from ALL ingredient types
          key = key.replace(/\s*-\s*.+$/, '').trim();
          
          console.log(`[KITCHEN SUMMARY] Ingredient type: "${ingredient.type}" -> cleaned: "${key}", qty: ${qty}`);
          
          // Handle bone type ingredients stored as types (boneless/original as standalone)
          if (lower === 'boneless' || lower === 'original') {
            // These are bone types stored as ingredient types - treat as wings
            if (lower === 'boneless') {
              key = 'Boneless Wings';
            } else {
              key = 'Wings';
            }
            console.log(`[KITCHEN SUMMARY] Converted bone type to wings: "${key}" = ${qty}`);
            mainItems[normalizeKey(key)] = (mainItems[normalizeKey(key)] || 0) + qty;
          }
          // MAIN ITEMS: Proteins (wings, tenders, chicken, beef) - but NOT rice/salad/veggie combos or dipping sauces
          else if ((lower.includes('wings') || lower.includes('tender') || 
                    lower.includes('grill chicken') || lower.includes('grilled chicken') ||
                    (lower.includes('beef') && !lower.includes('rice') && !lower.includes('veggie'))) &&
                   !lower.includes('rice') && !lower.includes('salad') && !lower.includes('veggie') &&
                   !lower.includes('ranch') && !lower.includes('bleu') && !lower.includes('blue cheese')) {
            // Get bone type from customization ONLY for wings/tenders
            if (lower.includes('wings') || lower.includes('tender')) {
              const itemBoneType = boneType || (entrees && entrees[0]?.boneType);
              
              // Add bone type prefix if present and not "Original"
              if (itemBoneType && itemBoneType.toLowerCase() !== 'original') {
                key = `${itemBoneType} ${key}`;
              }
            }
            
            console.log(`[KITCHEN SUMMARY] Adding to MAIN: "${key}" = ${qty}`);
            mainItems[normalizeKey(key)] = (mainItems[normalizeKey(key)] || 0) + qty;
          }
          // Fries and their substitutions
          else if (lower.includes('fries') || lower.includes('wedge')) {
            if (friesExchange) {
              key = friesExchange.name;
              if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
              if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
              friesReplaced = true;
            }
            sidesItems[normalizeKey(key)] = (sidesItems[normalizeKey(key)] || 0) + qty;
          }
          // Salads
          else if (lower.includes('salad') || lower.includes('caesar')) {
            if (saladType) {
              key = saladType;
              saladReplaced = true;
            }
            sidesItems[normalizeKey(key)] = (sidesItems[normalizeKey(key)] || 0) + qty;
          }
          // All other sides: rice, veggies, chips, smiley, potato, etc.
          else if (lower.includes('rice') || lower.includes('veggie') || 
                   lower.includes('chips') || lower.includes('chip') || lower.includes('stix') || 
                   lower.includes('mozzarella') || lower.includes('smiley') || lower.includes('potato') ||
                   lower.includes('drumstick')) {
            sidesItems[normalizeKey(key)] = (sidesItems[normalizeKey(key)] || 0) + qty;
          }
        });
        
        // Process dipping sauces from entrees (for combos) or main item
        if (entrees && entrees.length > 0) {
          entrees.forEach((entree: any) => {
            if (entree.dippingSauce) {
              const sauceName = typeof entree.dippingSauce === 'string'
                ? entree.dippingSauce
                : entree.dippingSauce.displayName || entree.dippingSauce.name || entree.dippingSauce;
              if (sauceName && String(sauceName).toUpperCase() !== 'NONE') {
                const normalizedSauce = normalizeKey(sauceName);
                dippingsItems[normalizedSauce] = (dippingsItems[normalizedSauce] || 0) + itemQty;
              }
            }
          });
        } else if (dippingSauce) {
          const sauceName = typeof dippingSauce === 'string' ? dippingSauce : dippingSauce.displayName || dippingSauce.name || dippingSauce;
          if (sauceName && String(sauceName).toUpperCase() !== 'NONE') {
            const normalizedSauce = normalizeKey(sauceName);
            dippingsItems[normalizedSauce] = (dippingsItems[normalizedSauce] || 0) + itemQty;
          }
        }
        
        // Add side substitutions that weren't replaced
        if (friesExchange && !friesReplaced) {
          let key = friesExchange.name;
          if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
          if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
          sidesItems[key] = (sidesItems[key] || 0) + itemQty;
        }
        
        if (saladType && !saladReplaced) {
          sidesItems[saladType] = (sidesItems[saladType] || 0) + itemQty;
        }
        
        // Add drinks for items with kitchen ingredients
        if (drink) {
          const drinkName = drink.displayName || drink.name || drink;
          if (drinkName && String(drinkName).toUpperCase() !== 'NONE') {
            const normalizedDrink = normalizeKey(drinkName);
            drinksItems[normalizedDrink] = (drinksItems[normalizedDrink] || 0) + itemQty;
          }
        }
      }
      // Fallback: No kitchen ingredients - categorize by item name
      else {
        console.log(`[KITCHEN SUMMARY] No kitchen ingredients for item: ${itemName}, using name-based categorization`);
        
        // Skip if this is just a bone type or entree label (not actual food)
        const skipTerms = ['entree', 'combo', 'meal'];
        const shouldSkip = skipTerms.some(term => itemName.toLowerCase().includes(term) && 
                                                   !itemName.toLowerCase().includes('wings') && 
                                                   !itemName.toLowerCase().includes('tender'));
        
        if (shouldSkip) {
          console.log(`[KITCHEN SUMMARY] Skipping entree label: ${itemName}`);
        }
        // Categorize wings/tenders
        else if (itemName.includes('wings') || itemName.includes('tender') || itemName.includes('chicken')) {
          let key = item.menuItem?.name || item.menuItemName || item.name;
          // Strip flavor from name
          key = key.replace(/\s*-\s*.+$/, '').trim();
          
          // Try to get actual wing count from item data
          const wingCount = item.menuItem?.defaultQuantity || item.defaultQuantity || itemQty;
          
          // Add bone type if present and not "Original"
          if (boneType && boneType.toLowerCase() !== 'original') {
            key = `${boneType} ${key}`;
          }
          mainItems[key] = (mainItems[key] || 0) + wingCount;
        }
        // Drinks
        else if (itemName.includes('coke') || itemName.includes('sprite') || itemName.includes('tea') || 
                 itemName.includes('juice') || itemName.includes('water') || itemName.includes('drink')) {
          const key = item.menuItem?.name || item.menuItemName || item.name;
          const normalizedDrink = normalizeKey(key);
          drinksItems[normalizedDrink] = (drinksItems[normalizedDrink] || 0) + itemQty;
        }
        // Sides
        else if (itemName.includes('fries') || itemName.includes('rice') || itemName.includes('veggie') ||
                 itemName.includes('chips') || itemName.includes('salad') || itemName.includes('stix') ||
                 itemName.includes('mozzarella')) {
          const key = item.menuItem?.name || item.menuItemName || item.name;
          sidesItems[key] = (sidesItems[key] || 0) + itemQty;
        }
        // Default to main
        else {
          const key = item.menuItem?.name || item.menuItemName || item.name;
          if (key) {
            mainItems[key] = (mainItems[key] || 0) + itemQty;
          }
        }
        
        // Still add customizations even without kitchen ingredients
        if (dippingSauce) {
          const sauceName = typeof dippingSauce === 'string' ? dippingSauce : dippingSauce.displayName || dippingSauce.name || dippingSauce;
          if (sauceName && String(sauceName).toUpperCase() !== 'NONE') {
            const normalizedSauce = normalizeKey(sauceName);
            dippingsItems[normalizedSauce] = (dippingsItems[normalizedSauce] || 0) + itemQty;
          }
        }
        
        if (drink) {
          const drinkName = drink.displayName || drink.name || drink;
          if (drinkName && String(drinkName).toUpperCase() !== 'NONE') {
            drinksItems[drinkName] = (drinksItems[drinkName] || 0) + itemQty;
          }
        }
        
        if (friesExchange) {
          let key = friesExchange.name;
          if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
          if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
          sidesItems[key] = (sidesItems[key] || 0) + itemQty;
        }
      }
    });
    
    let summary = '';
    
    if (Object.keys(mainItems).length > 0) {
      summary += 'MAIN:\n';
      Object.entries(mainItems).forEach(([type, total]) => {
        summary += `- ${total} ${type.toUpperCase()}\n`;
      });
      summary += '\n';
    }
    
    if (Object.keys(sidesItems).length > 0) {
      summary += 'SIDES:\n';
      Object.entries(sidesItems).forEach(([type, total]) => {
        summary += `- ${total} ${type.toUpperCase()}\n`;
      });
      summary += '\n';
    }
    
    if (Object.keys(dippingsItems).length > 0) {
      summary += 'DIPPINGS:\n';
      Object.entries(dippingsItems).forEach(([type, total]) => {
        summary += `- ${total} ${type.toUpperCase()}\n`;
      });
      summary += '\n';
    }
    
    if (Object.keys(drinksItems).length > 0) {
      summary += 'DRINKS:\n';
      Object.entries(drinksItems).forEach(([type, total]) => {
        summary += `- ${total} ${type.toUpperCase()}\n`;
      });
    }
    
    return summary;
  }

  private padColumns(columns: string[], widths: number[]): string {
    let line = '';
    columns.forEach((col, i) => {
      const width = widths[i] || 10;
      if (i === columns.length - 1) {
        // Right align last column (price)
        line += col.padStart(width);
      } else {
        line += col.padEnd(width);
      }
    });
    return line + '\n';
  }

  isConnected(): boolean {
    return this.connected;
  }
}

export default ThermalPrinterService.getInstance();
