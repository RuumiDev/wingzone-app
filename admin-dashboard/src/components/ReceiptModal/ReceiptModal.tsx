import React, { useState } from 'react';
import { Modal, ModalHeader, ModalBody, Button, Alert } from 'reactstrap';
import { QRCodeSVG } from 'qrcode.react';
import thermalPrinter from '../../services/thermalPrinter';
import './ReceiptModal.scss';

interface ReceiptModalProps {
  isOpen: boolean;
  toggle: () => void;
  order: any;
}

const ReceiptModal: React.FC<ReceiptModalProps> = ({ isOpen, toggle, order }) => {
  const [printMethod, setPrintMethod] = useState<'thermal' | 'browser'>('thermal');
  const [printerStatus, setPrinterStatus] = useState<string>('');
  const [availablePrinters, setAvailablePrinters] = useState<string[]>([]);
  const [selectedPrinter, setSelectedPrinter] = useState<string>('');
  const [isPrintingGroup, setIsPrintingGroup] = useState<boolean>(false);

  const handleThermalPrint = async (receiptContent?: string) => {
    try {
      setPrinterStatus('Connecting to QZ Tray...');
      
      const success = await thermalPrinter.printReceipt(order, {
        printerName: selectedPrinter || undefined,
        paperWidth: 80
      }, receiptContent);
      
      if (success) {
        setPrinterStatus('✓ Print sent successfully!');
        setTimeout(() => setPrinterStatus(''), 3000);
      } else {
        setPrinterStatus('✗ Print failed. Trying browser print...');
        setTimeout(() => handleBrowserPrint(), 1000);
      }
    } catch (error) {
      console.error('Thermal print error:', error);
      setPrinterStatus('✗ QZ Tray not available. Using browser print...');
      setTimeout(() => handleBrowserPrint(), 1000);
    }
  };

  const handlePrintGroupReceipts = async () => {
    if (!order.members || order.members.length === 0) {
      setPrinterStatus('✗ No members found in group order');
      return;
    }

    setIsPrintingGroup(true);
    setPrinterStatus(`Printing master receipt + ${order.members.length} member receipts...`);

    try {
      // Step 1: Print master receipt (all items from all members)
      await handleThermalPrint();
      await new Promise(resolve => setTimeout(resolve, 1500)); // Wait between prints

      // Step 2: Print individual member receipts
      for (let i = 0; i < order.members.length; i++) {
        const member = order.members[i];
        setPrinterStatus(`Printing receipt ${i + 1}/${order.members.length} for ${member.name || member.userName}...`);
        
        // Create individual member order object
        const memberOrder = {
          ...order,
          userName: member.name || member.userName,
          userId: member.userId,
          items: member.cartItems || [],
          total: (member.cartItems || []).reduce((sum: number, item: any) => sum + (item.subtotal || 0), 0),
          subtotal: (member.cartItems || []).reduce((sum: number, item: any) => sum + (item.subtotal || 0), 0),
          isMemberReceipt: true,
          memberIndex: i + 1,
          totalMembers: order.members.length,
          isHost: member.userId === order.hostId || member.isHost
        };

        await thermalPrinter.printReceipt(memberOrder, {
          printerName: selectedPrinter || undefined,
          paperWidth: 80
        });

        // Wait between member receipts
        if (i < order.members.length - 1) {
          await new Promise(resolve => setTimeout(resolve, 1500));
        }
      }

      setPrinterStatus(`✓ Successfully printed ${order.members.length + 1} receipts!`);
      setTimeout(() => {
        setPrinterStatus('');
        setIsPrintingGroup(false);
      }, 3000);
    } catch (error) {
      console.error('Group print error:', error);
      setPrinterStatus('✗ Error printing group receipts');
      setIsPrintingGroup(false);
    }
  };

  const handleBrowserPrint = () => {
    const printWindow = window.open('', '_blank', 'width=800,height=600');
    if (!printWindow) return;

    const receiptContent = document.getElementById('receipt-print');
    if (!receiptContent) return;

    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Receipt Print</title>
          <style>
            * {
              margin: 0;
              padding: 0;
              box-sizing: border-box;
            }
            @page {
              size: 80mm 297mm;
              margin: 0;
            }
            body {
              width: 80mm;
              margin: 0;
              padding: 0;
              font-family: 'Courier New', monospace;
            }
            .receipt-container {
              width: 100%;
              padding: 10px;
              font-family: 'Courier New', monospace;
              color: #000;
              font-size: 16px;
              font-weight: 700;
              line-height: 1.6;
            }
            .receipt-header { text-align: center; margin-bottom: 10px; }
            .order-type-banner {
              font-size: 20px;
              font-weight: 900;
              margin: 10px 0;
              letter-spacing: 5px;
              text-align: center;
              text-decoration: underline;
              text-decoration-thickness: 2px;
            }
            .table-info, .lobby-info, .host-info, .customer-info {
              font-size: 16px;
              font-weight: 700;
              margin: 4px 0;
              text-align: center;
            }
            .divider-line { display: none; }
            .restaurant-name {
              font-size: 18px;
              font-weight: 900;
              margin: 8px 0;
              text-align: center;
            }
            .reg-number, .restaurant-address {
              font-size: 14px;
              margin: 2px 0;
              text-align: center;
            }
            .receipt-divider { display: none; }
            .order-info {
              margin: 10px 0;
              font-size: 14px;
              font-weight: 700;
            }
            .date-time-row, .order-id-row, .status-row {
              display: flex;
              justify-content: space-between;
              margin: 6px 0;
            }
            .items-section { margin: 15px 0; }
            .section-title {
              font-size: 10px;
              font-weight: 900;
              text-align: center;
              margin: 10px 0;
              letter-spacing: 1px;
            }
            .section-subtitle {
              font-size: 12px;
              text-align: center;
              margin: 5px 0;
            }
            .items-header {
              display: flex;
              border-bottom: 2px solid #000;
              padding-bottom: 8px;
              margin-bottom: 10px;
              font-size: 16px;
              font-weight: 900;
            }
            .qty-col { width: 50px; }
            .item-col { flex: 1; padding: 0 15px; }
            .price-col { width: 100px; text-align: right; }
            .receipt-item { margin-bottom: 10px; }
            .item-row {
              display: flex;
              border-bottom: 1px dashed #ddd;
              padding: 8px 0;
              font-size: 16px;
              font-weight: 900;
            }
            .item-row .qty { width: 50px; }
            .item-row .name { flex: 1; padding: 0 15px; }
            .item-row .price { width: 100px; text-align: right; }
            .modifiers {
              margin-left: 65px;
              font-size: 13px;
              font-weight: 700;
              line-height: 1.6;
              margin-top: 8px;
            }
            .highlight-dip, .highlight-drink { font-weight: 900; }
            .kitchen-summary {
              background-color: #F8F9FA;
              padding: 15px;
              margin: 10px 0;
              font-size: 14px;
              line-height: 1.8;
            }
            .summary-category { font-weight: 900; margin-top: 10px; }
            .summary-line { margin-left: 10px; }
            .packing-box {
              border: 2px solid #000;
              padding: 10px;
              margin: 10px 0;
            }
            .box-header {
              font-weight: 900;
              font-size: 16px;
              margin-bottom: 8px;
            }
            .box-item { margin: 8px 0; }
            .item-details { margin-left: 20px; font-size: 13px; }
            .totals-section { margin: 15px 0; }
            .total-row {
              display: flex;
              justify-content: space-between;
              margin: 6px 0;
              font-size: 14px;
              font-weight: 700;
            }
            .grand-total {
              font-size: 18px;
              font-weight: 900;
              margin: 10px 0;
            }
            .paid-stamp {
              text-align: center;
              font-size: 20px;
              font-weight: 900;
              margin: 10px 0;
            }
            .payment-info {
              text-align: center;
              font-size: 14px;
              margin: 5px 0;
            }
            .receipt-qr {
              text-align: center;
              margin: 20px 0;
            }
            .receipt-qr svg {
              width: 120px;
              height: 120px;
              border: 2px solid #000;
              border-radius: 4px;
              padding: 5px;
            }
            .qr-label {
              font-size: 12px;
              margin-top: 8px;
            }
            .receipt-footer { text-align: center; margin: 15px 0; }
            .auth-section { margin: 15px 0; }
            .auth-code {
              font-size: 16px;
              font-weight: 900;
              text-align: center;
              margin: 10px 0;
            }
            .wifi-info, .social-info {
              font-size: 13px;
              margin: 5px 0;
            }
            .total-summary {
              text-align: center;
              font-size: 14px;
              font-weight: 700;
              margin: 10px 0;
            }
          </style>
        </head>
        <body>
          ${receiptContent.innerHTML}
        </body>
      </html>
    `);

    printWindow.document.close();
    printWindow.focus();

    setTimeout(() => {
      printWindow.print();
      printWindow.close();
    }, 250);
  };

  const handlePrint = () => {
    if (printMethod === 'thermal') {
      handleThermalPrint();
    } else {
      handleBrowserPrint();
    }
  };

  const loadPrinters = async () => {
    try {
      const printers = await thermalPrinter.getPrinters();
      setAvailablePrinters(printers);
      if (printers.length > 0 && !selectedPrinter) {
        // Auto-select thermal printer if found
        const thermalPrinter = printers.find(p => 
          p.toLowerCase().includes('thermal') || 
          p.toLowerCase().includes('pos') ||
          p.toLowerCase().includes('receipt')
        );
        setSelectedPrinter(thermalPrinter || printers[0]);
      }
    } catch (error) {
      console.error('Failed to load printers:', error);
      setPrinterStatus('QZ Tray not available');
    }
  };

  if (!order) return null;

  // Debug: Log the order structure
  console.log('=== RECEIPT MODAL DEBUG ===');
  console.log('Full order:', order);
  console.log('Order items:', order.items);
  if (order.items && order.items[0]) {
    console.log('First item:', order.items[0]);
    console.log('First item keys:', Object.keys(order.items[0]));
    console.log('First item.menuItem:', order.items[0].menuItem);
    if (order.items[0].menuItem) {
      console.log('menuItem keys:', Object.keys(order.items[0].menuItem));
    }
  }

  const formatDate = (timestamp: any) => {
    if (!timestamp) return '';
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    return new Intl.DateTimeFormat('en-MY', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  };

  return (
    <Modal isOpen={isOpen} toggle={toggle} size="lg" className="receipt-modal">
      <ModalHeader toggle={toggle}>
        <i className="bi bi-receipt me-2"></i>
        Order Receipt
      </ModalHeader>
      <ModalBody>
        <div className="receipt-container" id="receipt-print">
          {/* Header with Logo */}
          <div className="receipt-header">
            <img src="/WingzoneLogo.png" alt="Wingzone Logo" style={{maxWidth: '200px', margin: '10px auto', display: 'block'}} />
            <h1 style={{fontSize: '24px', fontWeight: '900', letterSpacing: '8px', textAlign: 'center', margin: '10px 0'}}>W I N G Z O N E</h1>
            <div className="divider-line">========================================</div>
            <div className="order-type-banner">
              {order.isMemberReceipt 
                ? `MEMBER RECEIPT ${order.isHost ? '(HOST)' : ''}`
                : order.lobbyId || order.isGroupOrder || order.members
                ? 'MASTER GROUP RECEIPT'
                : order.orderType === 'dine-in' 
                ? 'DINE-IN' 
                : 'PICKUP'}
            </div>
            <div className="divider-line">========================================</div>
            {order.tableNumber && (
              <div className="table-info">TABLE: {order.tableNumber}</div>
            )}
            {(order.lobbyId || order.isGroupOrder || order.members) && !order.isMemberReceipt && (
              <>
                <div className="lobby-info">GROUP CODE: #{order.code || order.groupOrderCode}</div>
                <div className="host-info">HOST: {order.hostUserName || order.userName}</div>
                <div className="host-info">MEMBERS: {order.members?.length || order.memberCount || 0}</div>
              </>
            )}
            {order.isMemberReceipt && (
              <>
                <div className="lobby-info">GROUP CODE: #{order.code || order.groupOrderCode}</div>
                <div className="customer-info">MEMBER: {order.userName} {order.isHost ? '(HOST)' : ''}</div>
                <div className="customer-info">RECEIPT {order.memberIndex}/{order.totalMembers}</div>
              </>
            )}
            {!order.lobbyId && !order.isGroupOrder && !order.members && order.userName && order.orderType === 'carry-out' && (
              <div className="customer-info">CUST: {order.userName}</div>
            )}
            {order.deliveryAddress && (
              <div className="customer-info">DELIVERY TO: {order.deliveryAddress}</div>
            )}
          </div>

          <div className="receipt-divider"></div>

          {/* Order Info */}
          <div className="receipt-section order-info">
            <div className="date-time-row">
              <span>Date: {formatDate(order.createdAt)}</span>
              <span>Time: {new Date(order.createdAt?.toDate ? order.createdAt.toDate() : order.createdAt).toLocaleTimeString('en-MY', { hour: '2-digit', minute: '2-digit' })}</span>
            </div>
            <div className="order-id-row">
              <span>Ord#: {order.id?.substring(0, 8).toUpperCase()}</span>
            </div>
            {(order.lobbyId || order.isGroupOrder || order.members) && (
              <div className="status-row">
                <span>Status: PAID (Online Gateway)</span>
              </div>
            )}
          </div>

          <div className="receipt-divider"></div>

          {/* Items */}
          <div className="receipt-section items-section">
            <div className="divider-line">----------------------------------------</div>
            {(order.lobbyId || order.isGroupOrder || order.members) && !order.isMemberReceipt ? (
              <>
                <h4 className="section-title">&gt;&gt; MASTER ORDER - ALL ITEMS &lt;&lt;</h4>
                <p className="section-subtitle">(All members' items combined)</p>
              </>
            ) : (
              <div className="items-header">
                <span className="qty-col">QTY</span>
                <span className="item-col">ITEM</span>
                <span className="price-col">PRICE</span>
              </div>
            )}
            <div className="divider-line">----------------------------------------</div>
            {(() => {
              // For group orders, extract all items from members
              let itemsToShow = order.items || [];
              
              if ((order.lobbyId || order.isGroupOrder || order.members) && !order.isMemberReceipt && order.members) {
                itemsToShow = [];
                order.members.forEach((member: any) => {
                  (member.cartItems || []).forEach((item: any) => {
                    itemsToShow.push({
                      ...item,
                      memberName: member.name || member.userName,
                      isHost: member.userId === order.hostId || member.isHost
                    });
                  });
                });
              }

              return itemsToShow.map((item: any, index: number) => (
                <div key={index} className="receipt-item">
                  <div className="item-row">
                    <span className="qty">{item.quantity || 1}</span>
                    <span className="name">
                      {item.menuItem?.name || item.menuItemName || item.name || 'ITEM'}
                      {(item.menuItem?.price === 0 || item.price === 0) ? ' (COMBO)' : ''}
                      {item.memberName && !order.isMemberReceipt && (
                        <span style={{ fontSize: '0.85em', fontWeight: 'normal' }}>
                          {' '}[{item.memberName}{item.isHost ? ' - HOST' : ''}]
                        </span>
                      )}
                    </span>
                    <span className="price">{((item.menuItem?.price || item.price || 0) * (item.quantity || 1)).toFixed(2)}</span>
                  </div>
                {item.customization && (
                  <div className="modifiers">
                    {/* Show entree details if present */}
                    {item.customization.entrees && item.customization.entrees.length > 0 && (
                      <div className="entrees-list">
                        {item.customization.entrees.map((entree: any, eIdx: number) => (
                          <div key={eIdx} className="entree-detail">
                            <div><strong>&gt; Entree {eIdx + 1}: {entree.name || 'Wings'}</strong></div>
                            {entree.boneType && <div className="ms-2">&bull; {entree.boneType}</div>}
                            {entree.flavor && <div className="ms-2">&bull; <strong>Flavor: {entree.flavor}</strong></div>}
                            {entree.dippingSauce && <div className="ms-2 highlight-dip">&bull; <strong>DIP: {entree.dippingSauce.toUpperCase()}</strong></div>}
                            {entree.friesExchange && <div className="ms-2">&bull; Side: {entree.friesExchange.name}</div>}
                          </div>
                        ))}
                      </div>
                    )}
                    {/* Show single item customization */}
                    {item.customization.boneType && <div>&gt; {item.customization.boneType}</div>}
                    {item.customization.flavor && <div>&gt; <strong>Flavor: {item.customization.flavor}</strong></div>}
                    {item.customization.dippingSauce && <div className="highlight-dip">&gt; <strong>DIP: {item.customization.dippingSauce.toUpperCase()}</strong></div>}
                    {item.customization.sideDish && <div>&gt; Side: {item.customization.sideDish}</div>}
                    {item.customization.saladType && <div>&gt; Salad: {item.customization.saladType}</div>}
                    {item.customization.friesExchange && (
                      <div>
                        &gt; Side: {item.customization.friesExchange.name}
                        {item.customization.friesExchange.selectedSize === 'jumbo' && ' (Jumbo)'}
                        {item.customization.friesExchange.selectedFlavor && ` - ${item.customization.friesExchange.selectedFlavor}`}
                        {(() => {
                          const exchange = item.customization.friesExchange;
                          const price = exchange.selectedSize === 'jumbo' && exchange.jumboPrice !== null 
                            ? exchange.jumboPrice 
                            : exchange.regularPrice;
                          return price > 0 ? ` (+RM ${price.toFixed(2)})` : '';
                        })()}
                      </div>
                    )}
                    {/* Show drink selection */}
                    {item.customization.drink && (
                      <div className="highlight-drink">&gt; <strong>DRINK: {typeof item.customization.drink === 'string' ? item.customization.drink : item.customization.drink.displayName || item.customization.drink.name || item.customization.drink}</strong></div>
                    )}
                    {item.customization.specialInstructions && <div>&gt; {item.customization.specialInstructions}</div>}
                  </div>
                )}
              </div>
              ));
            })()}
            
            {/* Kitchen Summary for Individual Orders and Member Receipts */}
            {((!order.lobbyId && !order.isGroupOrder && !order.members) || order.isMemberReceipt) && (
              <>
                <div className="divider-line">========================================</div>
                <h4 className="section-title">&gt;&gt; SUMMARY &lt;&lt;</h4>
                <div className="kitchen-summary">
                  {(() => {
                    // Use order.items for summary
                    const itemsForSummary = order.items || [];
                    
                    // Categorized ingredient totals
                    const mainItems: Record<string, number> = {};
                    const sidesItems: Record<string, number> = {};
                    const dippingsItems: Record<string, number> = {};
                    const drinksItems: Record<string, number> = {};
                    
                    itemsForSummary.forEach((item: any) => {
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
                          }
                        else if (lower.includes('wings') || lower.includes('tenders') || 
                                 (lower.includes('boneless') && !lower.includes('rice') && !lower.includes('veggie'))) {
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
                          else if (lower.includes('chips') || lower.includes('rice') || lower.includes('stix') || lower.includes('mozzarella') || lower.includes('veggie')) {
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
                    
                    return (
                      <>
                        {Object.keys(mainItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>MAIN:</strong></div>
                            {Object.entries(mainItems).map(([type, total]) => (
                              <div key={type} className="summary-line">- {total} {type.toUpperCase()}</div>
                            ))}
                            <br />
                          </>
                        )}
                        {Object.keys(sidesItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>SIDES:</strong></div>
                            {Object.entries(sidesItems).map(([type, total]) => (
                              <div key={type} className="summary-line">- {total} {type.toUpperCase()}</div>
                            ))}
                            <br />
                          </>
                        )}
                        {Object.keys(dippingsItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>DIPPINGS:</strong></div>
                            {Object.entries(dippingsItems).map(([type, total]) => (
                              <div key={type} className="summary-line">- {total} {type.toUpperCase()}</div>
                            ))}
                            <br />
                          </>
                        )}
                        {Object.keys(drinksItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>DRINKS:</strong></div>
                            {Object.entries(drinksItems).map(([type, total]) => (
                              <div key={type} className="summary-line">- {total} {type.toUpperCase()}</div>
                            ))}
                          </>
                        )}
                      </>
                    );
                  })()}
                </div>
                <div className="divider-line">========================================</div>
              </>
            )}
            
            {/* Group Order Packing List - ENHANCED with more details */}
            {((order.lobbyId || order.isGroupOrder || order.members) && !order.isMemberReceipt && order.members) && (
              <>
                <div className="divider-line">========================================</div>
                {/* Kitchen Summary for Group Orders */}
                <h4 className="section-title">&gt;&gt; KITCHEN SUMMARY &lt;&lt;</h4>
                <div className="kitchen-summary">
                  {(() => {
                    // Collect all cart items from all members
                    const allMemberItems: any[] = [];
                    order.members.forEach((member: any) => {
                      if (member.cartItems) {
                        allMemberItems.push(...member.cartItems);
                      }
                    });

                    // Generate categorized summary using the same logic as thermal printer
                    const mainItems: Record<string, number> = {};
                    const sidesItems: Record<string, number> = {};
                    const dippingsItems: Record<string, number> = {};
                    const drinksItems: Record<string, number> = {};

                    allMemberItems.forEach((item: any) => {
                      const itemQty = item.quantity || 1;
                      const kitchen = item.menuItem?.kitchenIngredients || item.kitchenIngredients;
                      const boneType = item.customization?.boneType;
                      const friesExchange = item.customization?.friesExchange;
                      const saladType = item.customization?.saladType;

                      let friesReplaced = false;
                      let saladReplaced = false;

                      if (kitchen?.ingredients) {
                        kitchen.ingredients.forEach((ingredient: any) => {
                          const qty = ingredient.quantity * itemQty;
                          let key = ingredient.type;
                          const lower = key.toLowerCase();

                          // Handle requiresSelection (wings, tenders)
                          if (ingredient.requiresSelection && boneType) {
                            if (boneType.toLowerCase() !== 'original') {
                              key = `${boneType} ${key}`;
                            }
                            mainItems[key] = (mainItems[key] || 0) + qty;
                          }
                          // Main items: wings, tenders, boneless (NOT rice/veggie combos)
                          else if (lower.includes('wings') || lower.includes('tenders') || 
                                   (lower.includes('boneless') && !lower.includes('rice') && !lower.includes('veggie'))) {
                            mainItems[key] = (mainItems[key] || 0) + qty;
                          }
                          // Sides - Fries
                          else if (lower.includes('fries')) {
                            if (friesExchange) {
                              key = friesExchange.name;
                              if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
                              if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
                              friesReplaced = true;
                            }
                            sidesItems[key] = (sidesItems[key] || 0) + qty;
                          }
                          // Sides - Salad
                          else if (lower.includes('salad')) {
                            if (saladType) {
                              key = saladType;
                              saladReplaced = true;
                            }
                            sidesItems[key] = (sidesItems[key] || 0) + qty;
                          }
                          // Sides - Other sides
                          else if (lower.includes('rice') || lower.includes('veggie') || lower.includes('beef') ||
                                   lower.includes('chips') || lower.includes('chip') || 
                                   lower.includes('stix') || lower.includes('mozzarella')) {
                            sidesItems[key] = (sidesItems[key] || 0) + qty;
                          }
                          // Dippings/Sauces
                          else if (lower.includes('dipping') || lower.includes('sauce') || 
                                   lower.includes('ranch') || lower.includes('bleu') || 
                                   lower.includes('dressing')) {
                            dippingsItems[key] = (dippingsItems[key] || 0) + qty;
                          }
                          // Drinks
                          else if (lower.includes('drink') || lower.includes('beverage') || 
                                   lower.includes('soda') || lower.includes('tea') || lower.includes('lemonade')) {
                            drinksItems[key] = (drinksItems[key] || 0) + qty;
                          }
                          // Default to main
                          else {
                            mainItems[key] = (mainItems[key] || 0) + qty;
                          }
                        });
                      }

                      // Add fries exchange if not replaced
                      if (friesExchange && !friesReplaced) {
                        let key = friesExchange.name;
                        if (friesExchange.selectedSize === 'jumbo') key += ' (Jumbo)';
                        if (friesExchange.selectedFlavor) key += ` - ${friesExchange.selectedFlavor}`;
                        sidesItems[key] = (sidesItems[key] || 0) + itemQty;
                      }

                      // Add salad if not replaced
                      if (saladType && !saladReplaced) {
                        sidesItems[saladType] = (sidesItems[saladType] || 0) + itemQty;
                      }
                    });
                    
                    return (
                      <>
                        {/* MAIN items */}
                        {Object.keys(mainItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>MAIN:</strong></div>
                            {Object.entries(mainItems).map(([type, total]) => (
                              <div key={type} className="summary-line">
                                <strong>  {total} {type.toUpperCase()}</strong>
                              </div>
                            ))}
                          </>
                        )}

                        {/* SIDES items */}
                        {Object.keys(sidesItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>SIDES:</strong></div>
                            {Object.entries(sidesItems).map(([type, total]) => (
                              <div key={type} className="summary-line">
                                <strong>  {total} {type.toUpperCase()}</strong>
                              </div>
                            ))}
                          </>
                        )}

                        {/* DIPPINGS items */}
                        {Object.keys(dippingsItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>DIPPINGS:</strong></div>
                            {Object.entries(dippingsItems).map(([type, total]) => (
                              <div key={type} className="summary-line">
                                <strong>  {total} {type.toUpperCase()}</strong>
                              </div>
                            ))}
                          </>
                        )}

                        {/* DRINKS items */}
                        {Object.keys(drinksItems).length > 0 && (
                          <>
                            <div className="summary-category"><strong>DRINKS:</strong></div>
                            {Object.entries(drinksItems).map(([type, total]) => (
                              <div key={type} className="summary-line">
                                <strong>  {total} {type.toUpperCase()}</strong>
                              </div>
                            ))}
                          </>
                        )}
                      </>
                    );
                  })()}
                </div>
                <div className="divider-line">=========</div>
                <div className="total-summary">
                  <div>TOTAL ITEMS: {order.members.reduce((sum: number, m: any) => {
                    return sum + (m.cartItems || []).reduce((itemSum: number, item: any) => itemSum + (item.quantity || 1), 0);
                  }, 0)}</div>
                  <div>TOTAL PAID:  RM {(order.total || order.totalAmount || 0).toFixed(2)}</div>
                </div>
                <div className="divider-line">=========</div>
                <h4 className="section-title">&gt;&gt; PACKING DISTRIBUTION LIST &lt;&lt;</h4>
                {order.members.map((member: any, index: number) => {
                  const isHost = member.userId === order.hostId || member.isHost;
                  return (
                  <div key={index} className="packing-box">
                    <div className="box-header">
                      [BOX {index + 1}] - {(member.name || member.userName || 'Member').toUpperCase()}
                      {isHost && ' (HOST)'}
                    </div>
                    {member.cartItems?.map((item: any, itemIndex: number) => (
                      <div key={itemIndex} className="box-item">
                        <div><strong>{item.quantity}x {item.menuItemName}</strong></div>
                        {item.customization && (
                          <div className="item-details ms-3">
                            {/* Show entree details for combos */}
                            {item.customization.entrees && item.customization.entrees.length > 0 && (
                              <>
                                {item.customization.entrees.map((entree: any, eIdx: number) => (
                                  <div key={eIdx} className="entree-detail">
                                    <div>&bull; <strong>Entree {eIdx + 1}: {entree.name || 'Wings'}</strong></div>
                                    {entree.boneType && <div className="ms-2">  - {entree.boneType}</div>}
                                    {entree.flavor && <div className="ms-2">  - <strong>Flavor: {entree.flavor}</strong></div>}
                                    {entree.dippingSauce && <div className="ms-2 highlight-dip">  - <strong>DIP: {entree.dippingSauce.toUpperCase()}</strong></div>}
                                    {entree.friesExchange && <div className="ms-2">  - Side: {entree.friesExchange.name}</div>}
                                  </div>
                                ))}
                              </>
                            )}
                            {/* Show single item customization */}
                            {item.customization.boneType && <div>&bull; {item.customization.boneType}</div>}
                            {item.customization.flavor && <div>&bull; <strong>Flavor: {item.customization.flavor}</strong></div>}
                            {item.customization.dippingSauce && <div className="highlight-dip">&bull; <strong>DIP: {item.customization.dippingSauce.toUpperCase()}</strong></div>}
                            {item.customization.sideDish && <div>&bull; Side: {item.customization.sideDish}</div>}
                            {item.customization.friesExchange && <div>&bull; Side: {item.customization.friesExchange.name}</div>}
                            {item.customization.drink && item.customization.drink.displayName && (
                              <div className="highlight-drink">&bull; <strong>DRINK: {item.customization.drink.displayName}</strong></div>
                            )}
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                );
                })}
              </>
            )}
          </div>

          <div className="receipt-divider"></div>

          {/* Totals - Show for individual orders and member receipts */}
          {((!order.lobbyId && !order.isGroupOrder && !order.members) || order.isMemberReceipt) && (
            <div className="receipt-section totals-section">
              <div className="divider-line">-----------------</div>
              <div className="total-row">
                <span className="label">SUBTOTAL</span>
                <span className="value">{(order.subtotal || order.totalAmount || 0).toFixed(2)}</span>
              </div>
              {order.tax !== undefined && (
                <div className="total-row">
                  <span className="label">SST (6%)</span>
                  <span className="value">{order.tax.toFixed(2)}</span>
                </div>
              )}
              <div className="divider-line">-----------------</div>
              <div className="total-row grand-total">
                <span className="label">TOTAL MYR</span>
                <span className="value">RM {(order.total || order.totalAmount || 0).toFixed(2)}</span>
              </div>
              <div className="divider-line">-----------------</div>
              {order.paymentStatus === 'paid' && (
                <div className="paid-stamp">*** P A I D ***</div>
              )}
              {order.paymentMethod && (
                <div className="payment-info">Pay Method: {order.paymentMethod}</div>
              )}
            </div>
          )}

          <div className="receipt-divider"></div>

          {/* QR Code */}
          <div className="receipt-qr">
            <QRCodeSVG 
              value={`WINGZONE-ORDER-${order.id}`} 
              size={120}
              level="M"
              includeMargin={true}
            />
            <p className="qr-label">Scan for order details</p>
          </div>

          {/* Footer */}
          <div className="receipt-footer">
            <div className="divider-line">========================================</div>
            <h2 className="restaurant-name">Zenith Certification Sdn Bhd</h2>
            <p className="reg-number">Reg: 199401032195 (317877-X)</p>
            <p className="restaurant-address">Lebuh Meru Raya, Bandar Meru Raya</p>
            <p className="restaurant-address">Ipoh, Perak</p>
            <br />
            {order.lobbyId && order.authCode && (
              <div className="auth-code" style={{fontWeight: '900', fontSize: '16px'}}>AUTH CODE: {order.authCode}</div>
            )}
            <p className="wifi-info">Wifi: wingzone123</p>
            <p className="social-info">IG/FB: Wing Zone Malaysia</p>
          </div>
        </div>

        <div className="receipt-actions print-hide">
          {printerStatus && (
            <Alert color={printerStatus.includes('✓') ? 'success' : 'info'} className="mb-3">
              {printerStatus}
            </Alert>
          )}
          
          <div className="mb-3">
            <div className="btn-group w-100 mb-2">
              <Button 
                color={printMethod === 'thermal' ? 'primary' : 'outline-primary'}
                onClick={() => setPrintMethod('thermal')}
              >
                <i className="bi bi-receipt me-2"></i>
                Thermal Printer
              </Button>
              <Button 
                color={printMethod === 'browser' ? 'primary' : 'outline-primary'}
                onClick={() => setPrintMethod('browser')}
              >
                <i className="bi bi-printer me-2"></i>
                Browser Print
              </Button>
            </div>
            
            {printMethod === 'thermal' && (
              <div className="printer-selection">
                <Button 
                  size="sm" 
                  color="secondary" 
                  outline 
                  onClick={loadPrinters}
                  className="mb-2 w-100"
                >
                  <i className="bi bi-arrow-clockwise me-2"></i>
                  Detect Printers
                </Button>
                {availablePrinters.length > 0 && (
                  <select 
                    className="form-select form-select-sm"
                    value={selectedPrinter}
                    onChange={(e) => setSelectedPrinter(e.target.value)}
                  >
                    {availablePrinters.map(printer => (
                      <option key={printer} value={printer}>{printer}</option>
                    ))}
                  </select>
                )}
                <small className="text-muted d-block mt-2">
                  <i className="bi bi-info-circle me-1"></i>
                  Requires QZ Tray running on your computer
                </small>
              </div>
            )}
          </div>
          
          <div className="d-flex gap-2">
            {order.members && order.members.length > 0 ? (
              <>
                <Button 
                  color="success" 
                  size="lg" 
                  onClick={handlePrintGroupReceipts} 
                  className="flex-fill"
                  disabled={isPrintingGroup}
                >
                  <i className="bi bi-printer-fill me-2"></i>
                  {isPrintingGroup ? 'Printing...' : `Print Master + ${order.members.length} Member Receipts`}
                </Button>
                <Button 
                  color="info" 
                  size="lg" 
                  onClick={handlePrint} 
                  className="flex-fill"
                  outline
                  disabled={isPrintingGroup}
                >
                  <i className="bi bi-printer me-2"></i>
                  Master Only
                </Button>
              </>
            ) : (
              <Button color="success" size="lg" onClick={handlePrint} className="flex-fill">
                <i className="bi bi-printer me-2"></i>
                Print Receipt
              </Button>
            )}
            <Button color="secondary" size="lg" onClick={toggle} outline disabled={isPrintingGroup}>
              Close
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
};

export default ReceiptModal;
