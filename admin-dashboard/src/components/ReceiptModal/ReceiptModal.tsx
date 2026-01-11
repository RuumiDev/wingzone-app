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

  const handleThermalPrint = async () => {
    try {
      setPrinterStatus('Connecting to QZ Tray...');
      
      const success = await thermalPrinter.printReceipt(order, {
        printerName: selectedPrinter || undefined,
        paperWidth: 80
      });
      
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
          {/* Header */}
          <div className="receipt-header">
            <div className="divider-line">========================================</div>
            <div className="order-type-banner">
              {order.lobbyId ? '** GROUP LOBBY **' : order.orderType === 'dine-in' ? '** DINE-IN **' : '** CARRY-OUT **'}
            </div>
            {order.tableNumber && (
              <div className="table-info">TABLE: {order.tableNumber}</div>
            )}
            {order.lobbyId && (
              <>
                <div className="lobby-info">LOBBY ID: #{order.code}</div>
                <div className="host-info">HOST: {order.hostUserName}</div>
              </>
            )}
            {!order.lobbyId && order.userName && order.orderType === 'carry-out' && (
              <div className="customer-info">CUST: {order.userName}</div>
            )}
            <div className="divider-line">========================================</div>
            <h2 className="restaurant-name">Zenith Certification Sdn Bhd</h2>
            <p className="reg-number">Reg: 199401032195 (317877-X)</p>
            <p className="restaurant-address">Lebuh Meru Raya,</p>
            <p className="restaurant-address">Bandar Meru Raya, Ipoh</p>
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
            {order.lobbyId && (
              <div className="status-row">
                <span>Status: PAID (Online Gateway)</span>
              </div>
            )}
          </div>

          <div className="receipt-divider"></div>

          {/* Items */}
          <div className="receipt-section items-section">
            <div className="divider-line">----------------------------------------</div>
            {order.lobbyId ? (
              <>
                <h4 className="section-title">&gt;&gt; KITCHEN PRODUCTION SUMMARY &lt;&lt;</h4>
                <p className="section-subtitle">(Cook these items together)</p>
              </>
            ) : (
              <div className="items-header">
                <span className="qty-col">QTY</span>
                <span className="item-col">ITEM</span>
                <span className="price-col">PRICE</span>
              </div>
            )}
            <div className="divider-line">----------------------------------------</div>
            {order.items?.map((item: any, index: number) => (
              <div key={index} className="receipt-item">
                <div className="item-row">
                  <span className="qty">{item.quantity || 1}</span>
                  <span className="name">{item.menuItem?.name || item.menuItemName || item.name || 'ITEM'}{(item.menuItem?.price === 0 || item.price === 0) ? ' (COMBO)' : ''}</span>
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
                            {entree.dippingSauce && <div className="ms-2 highlight-dip">&bull; <strong>*** DIP: {entree.dippingSauce.toUpperCase()} ***</strong></div>}
                            {entree.friesExchange && <div className="ms-2">&bull; Side: {entree.friesExchange.name}</div>}
                          </div>
                        ))}
                      </div>
                    )}
                    {/* Show single item customization */}
                    {item.customization.boneType && <div>&gt; {item.customization.boneType}</div>}
                    {item.customization.flavor && <div>&gt; <strong>Flavor: {item.customization.flavor}</strong></div>}
                    {item.customization.dippingSauce && <div className="highlight-dip">&gt; <strong>*** DIP: {item.customization.dippingSauce.toUpperCase()} ***</strong></div>}
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
            ))}
            
            {/* Kitchen Summary for Individual Orders */}
            {!order.lobbyId && order.items && (
              <>
                <div className="divider-line">========================================</div>
                <h4 className="section-title">&gt;&gt; SUMMARY &lt;&lt;</h4>
                <div className="kitchen-summary">
                  {(() => {
                    // Categorized ingredient totals
                    const mainItems: Record<string, number> = {};
                    const sidesItems: Record<string, number> = {};
                    const dippingsItems: Record<string, number> = {};
                    const drinksItems: Record<string, number> = {};
                    
                    order.items.forEach((item: any) => {
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
            {order.lobbyId && order.members && (
              <>
                <div className="divider-line">========================================</div>
                {/* Kitchen Summary for Group Orders */}
                <h4 className="section-title">&gt;&gt; SUMMARY &lt;&lt;</h4>
                <div className="kitchen-summary">
                  {(() => {
                    // Aggregate raw materials using flexible ingredient tags across all members
                    const ingredientTotals: Record<string, number> = {};
                    
                    order.members.forEach((member: any) => {
                      member.cartItems?.forEach((item: any) => {
                        const itemQty = item.quantity || 1;
                        const kitchen = item.menuItem?.kitchenIngredients || item.kitchenIngredients;
                        const boneType = item.customization?.boneType; // Get customer's selection
                        const friesExchange = item.customization?.friesExchange; // Get side substitution
                        const saladType = item.customization?.saladType; // Get salad choice
                        
                        let friesReplaced = false;
                        let saladReplaced = false;
                        
                        if (kitchen?.ingredients) {
                          kitchen.ingredients.forEach((ingredient: any) => {
                            // If ingredient requires selection (like wings), use customer's bone type choice
                            if (ingredient.requiresSelection && boneType) {
                              // Normalize "Original" to plain ingredient name since it's the default
                              let key;
                              if (boneType.toLowerCase() === 'original') {
                                key = ingredient.type;
                              } else {
                                key = `${boneType} ${ingredient.type}`;
                              }
                              ingredientTotals[key] = (ingredientTotals[key] || 0) + (ingredient.quantity * itemQty);
                            } 
                            // If ingredient is fries but customer chose a different side, use their choice
                            else if (ingredient.type.toLowerCase().includes('fries') && friesExchange) {
                              let key = friesExchange.name;
                              // Add size designation if jumbo
                              if (friesExchange.selectedSize === 'jumbo') {
                                key += ' (Jumbo)';
                              }
                              // Add flavor for Flavor Rub Fries
                              if (friesExchange.selectedFlavor) {
                                key += ` - ${friesExchange.selectedFlavor}`;
                              }
                              ingredientTotals[key] = (ingredientTotals[key] || 0) + (ingredient.quantity * itemQty);
                              friesReplaced = true;
                            }
                            // If ingredient is salad and customer chose a salad type, use their choice
                            else if (ingredient.type.toLowerCase().includes('salad') && saladType) {
                              ingredientTotals[saladType] = (ingredientTotals[saladType] || 0) + (ingredient.quantity * itemQty);
                              saladReplaced = true;
                            }
                            else {
                              // For non-selectable ingredients, just use the type
                              const key = ingredient.type;
                              ingredientTotals[key] = (ingredientTotals[key] || 0) + (ingredient.quantity * itemQty);
                            }
                          });
                        }
                        
                        // If customer chose a side but there was no fries ingredient to replace, add it anyway
                        if (friesExchange && !friesReplaced) {
                          let key = friesExchange.name;
                          if (friesExchange.selectedSize === 'jumbo') {
                            key += ' (Jumbo)';
                          }
                          if (friesExchange.selectedFlavor) {
                            key += ` - ${friesExchange.selectedFlavor}`;
                          }
                          ingredientTotals[key] = (ingredientTotals[key] || 0) + itemQty;
                        }
                        
                        // If customer chose a salad but there was no salad ingredient to replace, add it anyway
                        if (saladType && !saladReplaced) {
                          ingredientTotals[saladType] = (ingredientTotals[saladType] || 0) + itemQty;
                        }
                      });
                    });
                    
                    return (
                      <>
                        {Object.entries(ingredientTotals).map(([type, total]) => (
                          <div key={type} className="summary-line">
                            <strong>- {total}  {type.toUpperCase()}</strong>
                          </div>
                        ))}
                      </>
                    );
                  })()}
                </div>
                <div className="divider-line">=========</div>
                <div className="total-summary">
                  <div>TOTAL ITEMS: {order.items?.reduce((sum: number, item: any) => sum + (item.quantity || 1), 0)}</div>
                  <div>TOTAL PAID:  RM {(order.total || order.totalAmount || 0).toFixed(2)}</div>
                </div>
                <div className="divider-line">=========</div>
                <h4 className="section-title">&gt;&gt; PACKING DISTRIBUTION LIST &lt;&lt;</h4>
                {order.members.map((member: any, index: number) => (
                  <div key={index} className="packing-box">
                    <div className="box-header">[BOX {index + 1}] - {member.name.toUpperCase()}</div>
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
                                    {entree.dippingSauce && <div className="ms-2 highlight-dip">  - <strong>*** DIP: {entree.dippingSauce.toUpperCase()} ***</strong></div>}
                                    {entree.friesExchange && <div className="ms-2">  - Side: {entree.friesExchange.name}</div>}
                                  </div>
                                ))}
                              </>
                            )}
                            {/* Show single item customization */}
                            {item.customization.boneType && <div>&bull; {item.customization.boneType}</div>}
                            {item.customization.flavor && <div>&bull; <strong>Flavor: {item.customization.flavor}</strong></div>}
                            {item.customization.dippingSauce && <div className="highlight-dip">&bull; <strong>*** DIP: {item.customization.dippingSauce.toUpperCase()} ***</strong></div>}
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
                ))}
              </>
            )}
          </div>

          <div className="receipt-divider"></div>

          {/* Totals */}
          {!order.lobbyId && (
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
            {order.lobbyId && order.authCode && (
              <div className="auth-section">
                <div className="divider-line">========================================</div>
                <div className="auth-code">AUTH CODE: {order.authCode}</div>
                <div className="divider-line">========================================</div>
              </div>
            )}
            {!order.lobbyId && (
              <>
                <p className="wifi-info">Wifi: wingzone123</p>
                <p className="social-info">IG/FB: Wing Zone Malaysia</p>
                <div className="divider-line">========================================</div>
              </>
            )}
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
            <Button color="success" size="lg" onClick={handlePrint} className="flex-fill">
              <i className="bi bi-printer me-2"></i>
              Print Receipt
            </Button>
            <Button color="secondary" size="lg" onClick={toggle} outline>
              Close
            </Button>
          </div>
        </div>
      </ModalBody>
    </Modal>
  );
};

export default ReceiptModal;
