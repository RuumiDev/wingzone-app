import qz from 'qz-tray';

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
        await qz.websocket.connect();
      }
      this.connected = true;
      return true;
    } catch (error) {
      console.error('QZ Tray connection failed:', error);
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

  async printReceipt(order: any, config: ThermalPrintConfig = {}): Promise<boolean> {
    try {
      if (!this.connected) {
        const connected = await this.connect();
        if (!connected) {
          throw new Error('Could not connect to QZ Tray');
        }
      }

      // Get printer name
      let printerName = config.printerName;
      if (!printerName) {
        const printers = await this.getPrinters();
        if (printers.length === 0) {
          throw new Error('No printers found');
        }
        // Use first printer or find thermal printer
        printerName = printers.find(p => 
          p.toLowerCase().includes('thermal') || 
          p.toLowerCase().includes('pos') ||
          p.toLowerCase().includes('receipt')
        ) || printers[0];
      }

      // Generate ESC/POS commands
      const commands = this.generateReceiptCommands(order, config.paperWidth || 80);

      // Configure print job
      const printConfig = qz.configs.create(printerName, {
        encoding: 'UTF-8',
        size: { width: (config.paperWidth || 80) / 25.4, height: 11 } // Convert mm to inches
      });

      // Print
      await qz.print(printConfig, commands);
      return true;
    } catch (error) {
      console.error('Print failed:', error);
      return false;
    }
  }

  private generateReceiptCommands(order: any, paperWidth: number): string[] {
    const commands: string[] = [];
    const ESC = '\x1B';
    const GS = '\x1D';
    
    // Initialize printer
    commands.push(`${ESC}@`); // Initialize
    commands.push(`${ESC}a\x01`); // Center align
    
    // Header
    commands.push(`${ESC}!\x38`); // Double height + bold
    const orderType = order.lobbyId ? '** GROUP LOBBY **' : 
                     order.orderType === 'dine-in' ? '** DINE-IN **' : '** CARRY-OUT **';
    commands.push(orderType + '\n');
    commands.push(`${ESC}!\x00`); // Normal
    
    if (order.tableNumber) {
      commands.push(`TABLE: ${order.tableNumber}\n`);
    }
    
    if (order.lobbyId) {
      commands.push(`LOBBY ID: #${order.code}\n`);
      commands.push(`HOST: ${order.hostUserName}\n`);
    } else if (order.userName && order.orderType === 'carry-out') {
      commands.push(`CUST: ${order.userName}\n`);
    }
    
    // Restaurant info
    commands.push('\n');
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
    
    // Items header
    if (!order.lobbyId) {
      commands.push(`${ESC}!\x10`); // Bold
      commands.push(this.padColumns(['QTY', 'ITEM', 'PRICE'], [4, 20, 8]));
      commands.push(`${ESC}!\x00`); // Normal
      commands.push('--------------------------------\n');
    } else {
      commands.push(`${ESC}a\x01`); // Center
      commands.push(`${ESC}!\x10`); // Bold
      commands.push('>> SUMMARY <<\n');
      commands.push(`${ESC}!\x00`); // Normal
      commands.push(`${ESC}a\x00`); // Left
    }
    
    // Items
    if (order.items) {
      order.items.forEach((item: any) => {
        const qty = item.quantity || 1;
        const name = item.menuItem?.name || item.menuItemName || item.name || 'ITEM';
        const price = ((item.menuItem?.price || item.price || 0) * qty).toFixed(2);
        
        commands.push(`${ESC}!\x10`); // Bold
        commands.push(this.padColumns([qty.toString(), name, price], [4, 20, 8]));
        commands.push(`${ESC}!\x00`); // Normal
        
        // Customizations
        if (item.customization) {
          if (item.customization.boneType) {
            commands.push(`  > ${item.customization.boneType}\n`);
          }
          if (item.customization.flavor) {
            commands.push(`  > Flavor: ${item.customization.flavor}\n`);
          }
          if (item.customization.dippingSauce) {
            commands.push(`  > *** DIP: ${item.customization.dippingSauce.toUpperCase()} ***\n`);
          }
          if (item.customization.drink) {
            const drink = typeof item.customization.drink === 'string' 
              ? item.customization.drink 
              : item.customization.drink.displayName || item.customization.drink.name;
            commands.push(`  > DRINK: ${drink}\n`);
          }
        }
      });
    }
    
    // Kitchen summary for individual orders
    if (!order.lobbyId && order.items) {
      commands.push('\n');
      commands.push(`${ESC}a\x01`); // Center
      commands.push('>> SUMMARY <<\n');
      commands.push(`${ESC}a\x00`); // Left
      
      const summary = this.generateKitchenSummary(order.items);
      commands.push(summary);
    }
    
    // Totals
    if (!order.lobbyId) {
      commands.push('\n--------------------------------\n');
      commands.push(this.padColumns(['SUBTOTAL', '', (order.subtotal || order.totalAmount || 0).toFixed(2)], [10, 12, 10]));
      if (order.tax !== undefined) {
        commands.push(this.padColumns(['SST (6%)', '', order.tax.toFixed(2)], [10, 12, 10]));
      }
      commands.push('--------------------------------\n');
      commands.push(`${ESC}!\x20`); // Double width
      commands.push(this.padColumns(['TOTAL', '', `$${(order.total || order.totalAmount || 0).toFixed(2)}`], [6, 8, 10]));
      commands.push(`${ESC}!\x00`); // Normal
      commands.push('--------------------------------\n');
      
      if (order.paymentStatus === 'paid') {
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
      
      if (kitchen?.ingredients) {
        kitchen.ingredients.forEach((ingredient: any) => {
          const lower = ingredient.type.toLowerCase();
          let key = ingredient.type;
          let qty = ingredient.quantity * itemQty;
          
          if (ingredient.requiresSelection && boneType) {
            key = `${boneType} ${ingredient.type}`;
            mainItems[key] = (mainItems[key] || 0) + qty;
          } else if (lower.includes('wings') || lower.includes('tenders')) {
            mainItems[key] = (mainItems[key] || 0) + qty;
          } else if (lower.includes('fries')) {
            if (friesExchange) {
              key = friesExchange.name;
              if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
              if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
            }
            sidesItems[key] = (sidesItems[key] || 0) + qty;
          } else if (lower.includes('salad')) {
            if (saladType) key = saladType;
            sidesItems[key] = (sidesItems[key] || 0) + qty;
          } else if (lower.includes('chips') || lower.includes('rice') || lower.includes('stix')) {
            sidesItems[key] = (sidesItems[key] || 0) + qty;
          } else {
            mainItems[key] = (mainItems[key] || 0) + qty;
          }
        });
      }
      
      if (dippingSauce) {
        const sauceName = typeof dippingSauce === 'string' ? dippingSauce : dippingSauce.name || dippingSauce;
        dippingsItems[sauceName] = (dippingsItems[sauceName] || 0) + itemQty;
      }
      
      if (drink) {
        const drinkName = drink.displayName || drink.name || drink;
        drinksItems[drinkName] = (drinksItems[drinkName] || 0) + itemQty;
      }
    });
    
    let summary = '';
    
    if (Object.keys(mainItems).length > 0) {
      summary += 'MAIN:\n';
      Object.entries(mainItems).forEach(([type, total]) => {
        summary += `- ${total} ${type}\n`;
      });
      summary += '\n';
    }
    
    if (Object.keys(sidesItems).length > 0) {
      summary += 'SIDES:\n';
      Object.entries(sidesItems).forEach(([type, total]) => {
        summary += `- ${total} ${type}\n`;
      });
      summary += '\n';
    }
    
    if (Object.keys(dippingsItems).length > 0) {
      summary += 'DIPPINGS:\n';
      Object.entries(dippingsItems).forEach(([type, total]) => {
        summary += `- ${total} ${type}\n`;
      });
      summary += '\n';
    }
    
    if (Object.keys(drinksItems).length > 0) {
      summary += 'DRINKS:\n';
      Object.entries(drinksItems).forEach(([type, total]) => {
        summary += `- ${total} ${type}\n`;
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
