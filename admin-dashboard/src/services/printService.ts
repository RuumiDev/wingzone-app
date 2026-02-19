// Auto-print service for group order receipts
import { notificationService } from './notifications';
import { aggregateKitchenIngredients } from '../utils/kitchenIngredients';
import thermalPrinter from './thermalPrinter';
import { collection, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';

export interface GroupOrderParticipant {
  userId: string;
  userName: string;
  items: Array<{
    name: string;
    quantity: number;
    price: number;
    customizations?: any;
    menuItemId?: string;  // Add menu item ID to look up ingredients
  }>;
  total: number;
}

export interface GroupOrderForPrint {
  id: string;
  groupName: string;
  hostUserName: string;
  participants: GroupOrderParticipant[];
  orderDate: Date;
  total: number;
  orderType?: string;
  location?: string;
  paymentMethod?: string;
  paymentType?: string;
}

class PrintService {
  private autoPrintEnabled: boolean = true;
  private menuItemsCache: Map<string, any> = new Map();

  // Set auto-print preference
  setAutoPrintEnabled(enabled: boolean) {
    this.autoPrintEnabled = enabled;
  }

  // Fetch menu items from Firestore and cache them
  private async getMenuItems(): Promise<Map<string, any>> {
    if (this.menuItemsCache.size > 0) {
      return this.menuItemsCache;
    }

    try {
      const menuSnapshot = await getDocs(collection(db, 'menuItems'));
      menuSnapshot.forEach(doc => {
        const menuData = { id: doc.id, ...doc.data() };
        this.menuItemsCache.set(doc.id, menuData);
        // Also index by name for fallback matching
        if (menuData.name) {
          this.menuItemsCache.set(menuData.name, menuData);
        }
      });
      console.log(`[PrintService] Loaded ${menuSnapshot.size} menu items into cache`);
      return this.menuItemsCache;
    } catch (error) {
      console.error('[PrintService] Error fetching menu items:', error);
      return new Map();
    }
  }

  // Enrich cart items with full menu data (kitchen ingredients)
  private async enrichItemsWithMenuData(items: any[], menuItems?: any[]): Promise<any[]> {
    // If no menu items provided, try to fetch from Firestore
    let menuItemsArray = menuItems;
    if (!menuItemsArray || menuItemsArray.length === 0) {
      const menuItemsMap = await this.getMenuItems();
      menuItemsArray = Array.from(menuItemsMap.values());
    }
    
    console.log(`[PrintService] Enriching ${items.length} items with ${menuItemsArray?.length || 0} menu items`);
    
    return items.map((item, index) => {
      console.log(`[PrintService] Item ${index} raw data:`, item);
      
      // Check ALL possible fields where the name might be
      const itemName = item.menuItemName || item.name || item.itemName || 
                      item.menuItem?.name || item.item?.name || item.displayName ||
                      item.productName || item.title;
      
      console.log(`[PrintService] Item ${index} extracted name: "${itemName}" from fields:`, {
        menuItemName: item.menuItemName,
        name: item.name,
        itemName: item.itemName,
        menuItemDotName: item.menuItem?.name,
        allKeys: Object.keys(item)
      });
      
      if (!itemName) {
        console.error(`[PrintService] ✗ Item ${index} has NO name in any field!`, item);
        // Return item as-is but log the issue
        return item;
      }
      
      // Find matching menu item by name
      const menuItem = menuItemsArray?.find((m: any) => 
        m.name === itemName || m.name?.toLowerCase() === itemName?.toLowerCase()
      );
      
      if (menuItem) {
        console.log(`[PrintService] ✓ Enriched "${itemName}" with kitchen ingredients`);
        return {
          ...item,
          menuItem: menuItem,
          kitchenIngredients: menuItem.kitchenIngredients
        };
      }
      
      console.warn(`[PrintService] ✗ No menu match for: "${itemName}"`);
      return item;
    });
  }

  // Format receipt HTML for a single participant
  private formatParticipantReceipt(
    participant: GroupOrderParticipant,
    groupOrder: GroupOrderForPrint,
    participantNumber: number,
    totalParticipants: number,
    menuItems?: any[]  // Optional menu items for ingredient lookup
  ): string {
    console.log('=== PRINT SERVICE DEBUG ===');
    console.log('Participant items:', JSON.stringify(participant.items, null, 2));
    if (participant.items[0]) {
      console.log('First item structure:', participant.items[0]);
      console.log('First item name:', participant.items[0].name);
      console.log('First item menuItemName:', participant.items[0].menuItemName);
      console.log('First item price:', participant.items[0].price);
      console.log('First item customization:', participant.items[0].customization);
    }
    console.log('Menu items available:', menuItems?.length || 0);
    
    // Categorize items
    const meals: any[] = [];
    const sides: any[] = [];
    const drinks: string[] = [];
    const dippingSauces: string[] = [];
    const beverages: any[] = [];
    
    participant.items.forEach((item) => {
      console.log('Processing item:', item.name || item.menuItemName, 'with customization:', item.customization);
      
      // Check if it's a beverage category
      if (item.name?.includes('Coca-Cola') || item.name?.includes('Sprite') || 
          item.name?.includes('Tea') || item.name?.includes('Juice') || 
          item.name?.includes('Water')) {
        beverages.push(item);
      }
      // Check if it's a side
      else if (item.name?.includes('Fries') || item.name?.includes('Salad') || 
               item.name?.includes('Chips') || item.name?.includes('Rice') || 
               item.name?.includes('Stix') || item.name?.includes('Mozzarella')) {
        sides.push(item);
      }
      // Otherwise it's a meal
      else {
        meals.push(item);
      }
      
      // Extract drinks and sauces from customization
      if (item.customization?.drink) {
        drinks.push(item.customization.drink);
      }
      if (item.customization?.dippingSauce) {
        dippingSauces.push(item.customization.dippingSauce);
      }
    });

    // Combine duplicate drinks and sauces with counts
    const drinkCounts = drinks.reduce((acc: any, drink: string) => {
      acc[drink] = (acc[drink] || 0) + 1;
      return acc;
    }, {});

    const sauceCounts = dippingSauces.reduce((acc: any, sauce: string) => {
      acc[sauce] = (acc[sauce] || 0) + 1;
      return acc;
    }, {});

    // Organize kitchen summary into categories (matching ReceiptModal logic)
    const mainItems: Record<string, number> = {};
    const sidesItems: Record<string, number> = {};
    const dippingsItems: Record<string, number> = {};
    const drinksItems: Record<string, number> = {};

    // Process each item's kitchen ingredients
    participant.items.forEach((item: any) => {
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
            key = `${boneType} ${ingredient.type}`;
            mainItems[key] = (mainItems[key] || 0) + qty;
          }
          else if (lower.includes('wings') || lower.includes('tenders')) {
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
          }
          else if (lower.includes('salad')) {
            if (saladType) {
              key = saladType;
              saladReplaced = true;
            }
            sidesItems[key] = (sidesItems[key] || 0) + qty;
          }
          else if (lower.includes('chips') || lower.includes('rice') || lower.includes('stix') || 
                   lower.includes('mozzarella') || lower.includes('veggie') || lower.includes('bread')) {
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
        const drinkName = typeof drink === 'string' ? drink : drink.name || drink;
        drinksItems[drinkName] = (drinksItems[drinkName] || 0) + itemQty;
      }
    });

    // Format kitchen ingredients summary with categories
    const kitchenSummaryHtml = (Object.keys(mainItems).length > 0 || 
                                Object.keys(sidesItems).length > 0 || 
                                Object.keys(dippingsItems).length > 0 || 
                                Object.keys(drinksItems).length > 0) ? `
      <tr>
        <td colspan="2" style="padding: 20px 0 8px 0; border-top: 2px solid #000; text-align: center; font-weight: bold;">
          ========================================<br>
          >> KITCHEN SUMMARY <<</br>
          ========================================
        </td>
      </tr>
      <tr>
        <td colspan="2" style="padding: 10px; border: 2px solid #000;">
          ${Object.keys(mainItems).length > 0 ? `
            <strong>MAIN:</strong><br>
            ${Object.entries(mainItems).map(([ingredient, count]) => 
              `- ${count} ${ingredient}`
            ).join('<br>')}<br><br>
          ` : ''}
          ${Object.keys(sidesItems).length > 0 ? `
            <strong>SIDES:</strong><br>
            ${Object.entries(sidesItems).map(([ingredient, count]) => 
              `- ${count} ${ingredient}`
            ).join('<br>')}<br><br>
          ` : ''}
          ${Object.keys(dippingsItems).length > 0 ? `
            <strong>DIPPINGS:</strong><br>
            ${Object.entries(dippingsItems).map(([sauce, count]) => 
              `- ${count} ${sauce}`
            ).join('<br>')}<br><br>
          ` : ''}
          ${Object.keys(drinksItems).length > 0 ? `
            <strong>DRINKS:</strong><br>
            ${Object.entries(drinksItems).map(([drink, count]) => 
              `- ${count} ${drink}`
            ).join('<br>')}
          ` : ''}
        </td>
      </tr>
      <tr>
        <td colspan="2" style="padding: 8px 0; border-bottom: 2px solid #000; text-align: center;">
          ========================================
        </td>
      </tr>
    ` : '';

    // Format meals section with full details
    const mealsHtml = meals.length > 0 ? `
      ${meals.map((item, index) => {
        const customization = item.customization || {};
        const price = item.price || 0;
        
        return `
          <tr>
            <td style="padding: 8px 0 2px 0; vertical-align: top;">
              ${item.quantity}
            </td>
            <td style="padding: 8px 0 2px 0; text-align: right; vertical-align: top;">
              ${price.toFixed(2)}
            </td>
          </tr>
          <tr>
            <td colspan="2" style="padding: 2px 0 4px 0;">
              ${(item.menuItemName || item.name || 'ITEM').toUpperCase()}
            </td>
          </tr>
          ${customization.boneType ? `
            <tr>
              <td colspan="2" style="padding: 2px 0 2px 10px; font-size: 0.9em;">
                > ${customization.boneType.toUpperCase()}
              </td>
            </tr>
          ` : ''}
          ${customization.flavor ? `
            <tr>
              <td colspan="2" style="padding: 2px 0 2px 10px; font-size: 0.9em;">
                > Flavor: ${customization.flavor.toUpperCase().replace(/_/g, ' ')}
              </td>
            </tr>
          ` : ''}
          ${customization.dippingSauce ? `
            <tr>
              <td colspan="2" style="padding: 2px 0 2px 10px; background: #ffeb9c;">
                >  DIP: ${customization.dippingSauce.toUpperCase().replace(/_/g, ' ')} 
              </td>
            </tr>
          ` : ''}
          ${customization.friesExchange ? `
            <tr>
              <td colspan="2" style="padding: 2px 0 2px 10px; font-size: 0.9em;">
                > Side: ${customization.friesExchange.name}
              </td>
            </tr>
          ` : ''}
          ${customization.saladType ? `
            <tr>
              <td colspan="2" style="padding: 2px 0 2px 10px; font-size: 0.9em;">
                > Salad: ${customization.saladType}
              </td>
            </tr>
          ` : ''}
          ${customization.drink ? `
            <tr>
              <td colspan="2" style="padding: 2px 0 2px 10px; font-size: 0.9em;">
                > Drink: ${customization.drink}
              </td>
            </tr>
          ` : ''}
          <tr>
            <td colspan="2" style="padding: 8px 0; border-bottom: 1px dashed #999;"></td>
          </tr>
        `;
      }).join('')}
    ` : '';

    // All items are now shown with their details in mealsHtml
    // Kitchen summary shows aggregated ingredients

    return `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>Receipt - ${participant.userName}</title>
        <style>
          @media print {
            @page {
              size: 80mm auto; /* 80mm width, auto height for continuous thermal paper */
              margin: 0; /* No margins for thermal printer */
            }
            html, body {
              width: 80mm;
              margin: 0;
              padding: 0;
            }
            body {
              margin: 0 !important;
              padding: 5mm !important;
            }
          }
          body {
            font-family: 'Courier New', monospace;
            width: 80mm;
            max-width: 80mm;
            margin: 0;
            padding: 5mm;
            font-size: 11px;
            box-sizing: border-box;
          }
          .header {
            text-align: center;
            margin-bottom: 15px;
            border-bottom: 2px solid #000;
            padding-bottom: 10px;
          }
          .logo {
            font-size: 20px;
            font-weight: bold;
            margin-bottom: 5px;
          }
          .group-info {
            background: #f0f0f0;
            padding: 10px;
            margin: 10px 0;
            border-radius: 5px;
          }
          .participant-badge {
            background: #007bff;
            color: white;
            padding: 5px 10px;
            border-radius: 3px;
            display: inline-block;
            margin: 5px 0;
            font-weight: bold;
          }
          table {
            width: 100%;
            margin: 10px 0;
            border-collapse: collapse;
          }
          .total-row {
            font-weight: bold;
            font-size: 14px;
            border-top: 2px solid #000;
            padding-top: 10px;
          }
          .footer {
            text-align: center;
            margin-top: 20px;
            padding-top: 10px;
            border-top: 2px dashed #000;
            font-size: 10px;
          }
          .instruction {
            background: #fff3cd;
            padding: 8px;
            margin: 10px 0;
            border-radius: 3px;
            text-align: center;
            font-weight: bold;
          }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="logo">** CARRY-OUT **</div>
          <div style="margin: 10px 0;">
            Zenith Certification Sdn Bhd<br>
            <span style="font-size: 10px;">Reg: 199401032195 (317877-X)</span><br>
            <span style="font-size: 10px;">Lebuh Meru Raya,<br>Bandar Meru Raya, Ipoh</span>
          </div>
        </div>

        <div style="border-top: 1px dashed #999; border-bottom: 1px dashed #999; padding: 8px 0; margin: 10px 0;">
          <div><strong>Date:</strong> ${groupOrder.orderDate.toLocaleDateString()}  <strong>Time:</strong> ${groupOrder.orderDate.toLocaleTimeString()}</div>
          <div><strong>Ordr#:</strong> ${groupOrder.id.substring(0, 8).toUpperCase()}</div>
        </div>

        <div style="border-bottom: 2px solid #000; padding: 8px 0; margin: 10px 0;">
          <strong>Empl: ${participantNumber}</strong><br>
          <strong>FOR: ${participant.userName.toUpperCase()}</strong>
        </div>

        <div style="border-top: 2px solid #000; border-bottom: 2px solid #000; padding: 8px 0; margin: 10px 0;">
        </div>

        <table>
          <thead>
            <tr>
              <th style="text-align: left; padding: 4px 0;">QTY  ITEM</th>
              <th style="text-align: right; padding: 4px 0;">PRICE</th>
            </tr>
            <tr>
              <td colspan="2" style="border-bottom: 2px solid #000; padding-bottom: 4px;"></td>
            </tr>
          </thead>
          <tbody>
            ${mealsHtml}
          </tbody>
        </table>

        <table>
          <tbody>
            ${kitchenSummaryHtml}
          </tbody>
        </table>

        <table>
          <tr class="total-row">
            <td>SUB:</td>
            <td style="text-align: right;">RM ${participant.total.toFixed(2)}</td>
          </tr>
          <tr>
            <td>DISC:</td>
            <td style="text-align: right;">RM 0.00</td>
          </tr>
          <tr>
            <td>SST ON:</td>
            <td style="text-align: right;">RM 0.00</td>
          </tr>
          <tr style="border-top: 1px dashed #999;">
            <td><strong>TOT:</strong></td>
            <td style="text-align: right;"><strong>RM ${participant.total.toFixed(2)}</strong></td>
          </tr>
          <tr>
            <td colspan="2" style="padding-top: 5px;">Cash TEND: RM ${participant.total.toFixed(2)}</td>
          </tr>
        </table>

        <div style="text-align: center; margin: 20px 0; border-top: 1px dashed #999; border-bottom: 1px dashed #999; padding: 10px 0;">
          *** P A I D ***
        </div>

        <div style="text-align: center; margin: 10px 0;">
          *** REPRINT ***
        </div>

        <div class="footer">
          <div style="font-size: 10px;">
            Follow our Facebook<br>
            Instagram/TikTok<br>
            Wing Zone Malaysia
          </div>
          <div style="margin-top: 10px; font-size: 10px;">
            WIFI PASSWORD<br>
            Wingzone123
          </div>
        </div>
      </body>
      </html>
    `;
  }

  // Print individual receipts for all participants in a group order
  async printGroupOrderReceipts(groupOrder: GroupOrderForPrint, menuItems?: any[], printerName?: string, forcePrint: boolean = false) {
    console.log('[PrintService.printGroupOrderReceipts] ⚠️ CALLED - This should NOT be called for individual orders!');
    console.log('[PrintService] CODE VERSION: 2025-02-17-OLD-PATH');
    
    if (!this.autoPrintEnabled && !forcePrint) {
      console.log('Auto-print is disabled and forcePrint is false');
      return;
    }

    try {
      const totalParticipants = groupOrder.participants.length;
      console.log(`[PrintService] Printing MASTER + ${totalParticipants} member receipts using QZ Tray to printer: ${printerName || 'Default'}`);
      console.log(`[PrintService] Auto-print enabled: ${this.autoPrintEnabled}, Force print: ${forcePrint}`);

      // STEP 1: Print MASTER receipt first (all items combined)
      console.log('=== Printing MASTER RECEIPT ===');
      
      // Combine all items from all participants and enrich with menu data
      const allItems: any[] = [];
      groupOrder.participants.forEach(participant => {
        participant.items.forEach(item => {
          allItems.push({
            ...item,
            memberName: participant.userName
          });
        });
      });
      
      // Enrich items with kitchen ingredients from menu
      const enrichedAllItems = await this.enrichItemsWithMenuData(allItems, menuItems);

      const masterSuccess = await thermalPrinter.printReceipt(
        {
          id: groupOrder.id,
          groupOrderCode: groupOrder.id.substring(0, 8).toUpperCase(),
          code: groupOrder.id.substring(0, 8).toUpperCase(),
          hostUserName: groupOrder.hostUserName,
          userName: groupOrder.hostUserName,
          items: enrichedAllItems,
          total: groupOrder.total,
          subtotal: groupOrder.total,
          createdAt: groupOrder.orderDate,
          isGroupOrder: true,
          lobbyId: groupOrder.id,
          orderType: groupOrder.orderType,
          location: groupOrder.location,
          selectedLocation: groupOrder.location,
          paymentMethod: groupOrder.paymentMethod,
          members: await Promise.all(groupOrder.participants.map(async (p) => ({
            userName: p.userName,
            name: p.userName,
            cartItems: await this.enrichItemsWithMenuData(p.items, menuItems)
          }))),
          memberCount: totalParticipants
        },
        { printerName: printerName }
      );

      if (!masterSuccess) {
        console.error('Failed to print MASTER receipt');
      } else {
        console.log('✓ MASTER receipt printed');
      }

      // Delay before printing member receipts
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // STEP 2: Print each participant's individual receipt
      console.log(`=== Printing ${totalParticipants} MEMBER RECEIPTS ===`);
      
      for (let i = 0; i < groupOrder.participants.length; i++) {
        const participant = groupOrder.participants[i];
        
        console.log(`Printing member receipt ${i + 1}/${totalParticipants} for ${participant.userName}`);
        
        // Enrich participant items with menu data
        const enrichedParticipantItems = await this.enrichItemsWithMenuData(participant.items, menuItems);

        // Use thermal printer service to print member receipt
        const success = await thermalPrinter.printReceipt(
          {
            id: groupOrder.id,
            groupOrderCode: groupOrder.id.substring(0, 8).toUpperCase(),
            code: groupOrder.id.substring(0, 8).toUpperCase(),
            userName: participant.userName,
            items: enrichedParticipantItems,
            total: participant.total,
            subtotal: participant.total,
            createdAt: groupOrder.orderDate,
            isGroupOrder: true,
            isMemberReceipt: true,
            lobbyId: groupOrder.id,
            orderType: groupOrder.orderType,
            location: groupOrder.location,
            selectedLocation: groupOrder.location,
            paymentMethod: groupOrder.paymentMethod,
            memberIndex: i + 1,
            totalMembers: totalParticipants
          },
          { printerName: printerName },
          {
            name: participant.userName,
            cartItems: enrichedParticipantItems,
            index: i + 1,
            total: participant.total
          }
        );

        if (!success) {
          console.error(`Failed to print receipt for ${participant.userName}`);
        } else {
          console.log(`✓ Receipt printed for ${participant.userName}`);
        }

        // Small delay between prints to avoid printer issues
        await new Promise((resolve) => setTimeout(resolve, 1500));
      }

      console.log(`✓ Printed 1 MASTER + ${totalParticipants} MEMBER receipts for group order ${groupOrder.id}`);
    } catch (error) {
      console.error('Error printing group order receipts:', error);
      throw error;
    }
  }

  // Manual print for a specific participant
  printSingleParticipantReceipt(
    participant: GroupOrderParticipant,
    groupOrder: GroupOrderForPrint,
    participantNumber: number,
    totalParticipants: number,
    menuItems?: any[]
  ) {
    const receiptHtml = this.formatParticipantReceipt(
      participant,
      groupOrder,
      participantNumber,
      totalParticipants,
      menuItems
    );

    const printWindow = window.open('', '_blank', 'width=300,height=600');
    if (printWindow) {
      printWindow.document.write(receiptHtml);
      printWindow.document.close();
      printWindow.onload = () => {
        setTimeout(() => {
          printWindow.print();
        }, 250);
      };
    }
  }
}

export const printService = new PrintService();
