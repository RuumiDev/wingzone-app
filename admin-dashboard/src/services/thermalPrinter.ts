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
      const printConfig = qz.configs.create(printerName, {
        encoding: 'UTF-8',
        size: { width: (config.paperWidth || 80) / 25.4, height: 11 } // Convert mm to inches
      });

      // Print
      console.log('[QZ Tray] Sending to printer...');
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
    
    // Header separator
    commands.push('========================================\n');
    
    // Order type banner
    commands.push(`${ESC}!\x38`); // Double height + bold
    const orderType = order.lobbyId ? '** GROUP LOBBY **' : 
                     order.orderType === 'dine-in' ? '** DINE-IN **' : '** CARRY-OUT **';
    commands.push(orderType + '\n');
    
    // If individual member receipt, show member info prominently
    if (memberData) {
      commands.push(`${ESC}!\x30`); // Double size
      commands.push(`[MEMBER COPY]\n`);
      commands.push(`${ESC}!\x00`); // Normal
    }
    
    commands.push(`${ESC}!\x00`); // Normal
    
    if (order.tableNumber) {
      commands.push(`TABLE: ${order.tableNumber}\n`);
    }
    
    if (order.lobbyId) {
      commands.push(`LOBBY ID: #${order.code}\n`);
      if (memberData) {
        commands.push(`${ESC}!\x10`); // Bold
        commands.push(`MEMBER: ${memberData.name.toUpperCase()}\n`);
        commands.push(`${ESC}!\x00`); // Normal
      } else {
        commands.push(`HOST: ${order.hostUserName}\n`);
      }
    } else if (order.userName && order.orderType === 'carry-out') {
      commands.push(`CUST: ${order.userName}\n`);
    }
    
    commands.push('========================================\n');
    
    // Restaurant info
    commands.push(`${ESC}!\x10`); // Bold
    commands.push('Zenith Certification Sdn Bhd\n');
    commands.push(`${ESC}!\x00`); // Normal
    commands.push('Reg: 199401032195 (317877-X)\n');
    commands.push('Lebuh Meru Raya,\n');
    commands.push('Bandar Meru Raya, Ipoh\n\n');
    
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
              if (entree.flavor) {
                commands.push(`  - Flavor: ${entree.flavor}\n`);
              }
              if (entree.dippingSauce) {
                commands.push(`  - DIP: ${entree.dippingSauce.toUpperCase()} \n`);
              }
              if (entree.friesExchange) {
                commands.push(`  - Side: ${entree.friesExchange.name}\n\n`);
              }
            });
          }
          
          // Single item customization
          if (item.customization.boneType) {
            commands.push(`> ${item.customization.boneType}\n`);
          }
          if (item.customization.flavor) {
            commands.push(`> Flavor: ${item.customization.flavor}\n`);
          }
          if (item.customization.dippingSauce) {
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
            commands.push(`> DRINK: ${drink}\n`);
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
    if ((!order.lobbyId && order.items) || memberData) {
      commands.push('========================================\n');
      commands.push(`${ESC}a\x01`); // Center
      commands.push(`${ESC}!\x10`); // Bold
      commands.push('>> SUMMARY <<\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push(`${ESC}a\x00`); // Left
      
      const itemsForSummary = memberData ? memberData.cartItems : order.items;
      const summary = this.generateKitchenSummary(itemsForSummary);
      commands.push(summary);
      commands.push('========================================\n');
    }
    
    // Group order summary - match browser layout (master receipt only)
    if (order.lobbyId && order.items && !memberData) {
      commands.push('========================================\n');
      commands.push(`${ESC}a\x01`); // Center
      commands.push(`${ESC}!\x10`); // Bold
      commands.push('>> SUMMARY <<\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push(`${ESC}a\x00`); // Left
      
      // Generate ingredient totals for group orders
      const ingredientTotals: Record<string, number> = {};
      
      if (order.members) {
        order.members.forEach((member: any) => {
          member.cartItems?.forEach((item: any) => {
            const itemQty = item.quantity || 1;
            const kitchen = item.menuItem?.kitchenIngredients || item.kitchenIngredients;
            const boneType = item.customization?.boneType;
            const friesExchange = item.customization?.friesExchange;
            const saladType = item.customization?.saladType;
            
            let friesReplaced = false;
            let saladReplaced = false;
            
            if (kitchen?.ingredients) {
              kitchen.ingredients.forEach((ingredient: any) => {
                if (ingredient.requiresSelection && boneType) {
                  // Normalize "Original" to plain ingredient name since it's the default
                  let key;
                  if (boneType.toLowerCase() === 'original') {
                    key = ingredient.type;
                  } else {
                    key = `${boneType} ${ingredient.type}`;
                  }
                  ingredientTotals[key] = (ingredientTotals[key] || 0) + (ingredient.quantity * itemQty);
                } else if (ingredient.type.toLowerCase().includes('fries') && friesExchange) {
                  let key = friesExchange.name;
                  if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
                  if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
                  ingredientTotals[key] = (ingredientTotals[key] || 0) + (ingredient.quantity * itemQty);
                  friesReplaced = true;
                } else if (ingredient.type.toLowerCase().includes('salad') && saladType) {
                  ingredientTotals[saladType] = (ingredientTotals[saladType] || 0) + (ingredient.quantity * itemQty);
                  saladReplaced = true;
                } else {
                  const key = ingredient.type;
                  ingredientTotals[key] = (ingredientTotals[key] || 0) + (ingredient.quantity * itemQty);
                }
              });
            }
            
            if (friesExchange && !friesReplaced) {
              let key = friesExchange.name;
              if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
              if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
              ingredientTotals[key] = (ingredientTotals[key] || 0) + itemQty;
            }
            
            if (saladType && !saladReplaced) {
              ingredientTotals[saladType] = (ingredientTotals[saladType] || 0) + itemQty;
            }
          });
        });
      }
      
      // Print ingredient totals
      Object.entries(ingredientTotals).forEach(([type, total]) => {
        commands.push(`${ESC}!\x10`); // Bold
        commands.push(`- ${total}  ${type.toUpperCase()}\n`);
        commands.push(`${ESC}!\x00`); // Normal
      });
      
      commands.push('=========\n');
      const totalItems = order.items?.reduce((sum: number, item: any) => sum + (item.quantity || 1), 0) || 0;
      commands.push(`TOTAL ITEMS: ${totalItems}\n`);
      commands.push(`TOTAL PAID:  RM ${(order.total || order.totalAmount || 0).toFixed(2)}\n`);
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
            commands.push(`${ESC}!\x10`); // Bold
            commands.push(`${item.quantity}x ${item.menuItemName}\n`);
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
    if (order.lobbyId && order.authCode) {
      commands.push(`AUTH CODE: ${order.authCode}\n\n`);
    } else {
      commands.push('Wifi: wingzone123\n');
      commands.push('IG/FB: Wing Zone Malaysia\n');
    }
    
    // Cut paper
    commands.push('\n\n\n');
    commands.push(`${GS}V\x42\x00`); // Feed and cut
    
    return commands;
  }

  private generateKitchenSummary(items: any[]): string {
    const mainItems: Record<string, number> = {};
    const sidesItems: Record<string, number> = {};
    const dippingsItems: Record<string, number> = {};
    const drinksItems: Record<string, number> = {};
    
    items.forEach((item: any) => {
      const itemQty = item.quantity || 1;
      const kitchen = item.menuItem?.kitchenIngredients || item.kitchenIngredients;
      const boneType = item.customization?.boneType;
      const friesExchange = item.customization?.friesExchange;
      const saladType = item.customization?.saladType;
      const dippingSauce = item.customization?.dippingSauce;
      const drink = item.customization?.drink;
      
      let friesReplaced = false;
      let saladReplaced = false;
      
      if (kitchen?.ingredients) {
        kitchen.ingredients.forEach((ingredient: any) => {
          const lower = ingredient.type.toLowerCase();
          let key = ingredient.type;
          let qty = ingredient.quantity * itemQty;
          
          // Main items (wings, tenders)
          if (ingredient.requiresSelection && boneType) {
            // Normalize "Original" to plain ingredient name since it's the default
            if (boneType.toLowerCase() === 'original') {
              key = ingredient.type;
            } else {
              key = `${boneType} ${ingredient.type}`;
            }
            mainItems[key] = (mainItems[key] || 0) + qty;
          } else if (lower.includes('wings') || lower.includes('tenders')) {
            mainItems[key] = (mainItems[key] || 0) + qty;
          }
          // Sides (fries, salad, chips, rice, etc.)
          else if (lower.includes('fries')) {
            if (friesExchange) {
              key = friesExchange.name;
              if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
              if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
              friesReplaced = true;
            }
            sidesItems[key] = (sidesItems[key] || 0) + qty;
          } else if (lower.includes('salad')) {
            if (saladType) {
              key = saladType;
              saladReplaced = true;
            }
            sidesItems[key] = (sidesItems[key] || 0) + qty;
          } else if (lower.includes('chips') || lower.includes('rice') || lower.includes('stix') || lower.includes('mozzarella') || lower.includes('veggie')) {
            sidesItems[key] = (sidesItems[key] || 0) + qty;
          }
          // Everything else goes to main by default
          else {
            mainItems[key] = (mainItems[key] || 0) + qty;
          }
        });
      }
      
      // Add side substitutions even if no ingredient to replace
      if (friesExchange && !friesReplaced) {
        let key = friesExchange.name;
        if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
        if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
        sidesItems[key] = (sidesItems[key] || 0) + itemQty;
      }
      
      if (saladType && !saladReplaced) {
        sidesItems[saladType] = (sidesItems[saladType] || 0) + itemQty;
      }
      
      // Add dipping sauces
      if (dippingSauce) {
        const sauceName = typeof dippingSauce === 'string' ? dippingSauce : dippingSauce.name || dippingSauce;
        dippingsItems[sauceName] = (dippingsItems[sauceName] || 0) + itemQty;
      }
      
      // Add drinks
      if (drink) {
        const drinkName = drink.displayName || drink.name || drink;
        drinksItems[drinkName] = (drinksItems[drinkName] || 0) + itemQty;
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
