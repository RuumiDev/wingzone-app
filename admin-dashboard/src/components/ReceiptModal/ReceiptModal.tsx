import React from 'react';
import { Modal, ModalHeader, ModalBody, Button } from 'reactstrap';
import { QRCodeSVG } from 'qrcode.react';
import './ReceiptModal.scss';

interface ReceiptModalProps {
  isOpen: boolean;
  toggle: () => void;
  order: any;
}

const ReceiptModal: React.FC<ReceiptModalProps> = ({ isOpen, toggle, order }) => {
  const handlePrint = () => {
    window.print();
  };

  if (!order) return null;

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
                  <span className="name">{item.menuItemName || item.name}{item.price === 0 ? ' (COMBO)' : ''}</span>
                  <span className="price">{((item.price || 0) * (item.quantity || 1)).toFixed(2)}</span>
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
                    {item.customization.friesExchange && <div>&gt; Side: {item.customization.friesExchange.name}</div>}
                    {/* Show drink selection */}
                    {item.customization.drink && item.customization.drink.displayName && (
                      <div className="highlight-drink">&gt; <strong>DRINK: {item.customization.drink.displayName}</strong></div>
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
                <h4 className="section-title">&gt;&gt; KITCHEN SUMMARY &lt;&lt;</h4>
                <div className="kitchen-summary">
                  {(() => {
                    // Count entrees by type
                    const entreeCounts: Record<string, number> = {};
                    const sideCounts: Record<string, number> = {};
                    const breadCount = { count: 0 };
                    
                    order.items.forEach((item: any) => {
                      if (item.customization?.entrees) {
                        item.customization.entrees.forEach((entree: any) => {
                          const key = entree.boneType || entree.name || 'Wings';
                          entreeCounts[key] = (entreeCounts[key] || 0) + (item.quantity || 1);
                          
                          // Count sides/fries
                          if (entree.friesExchange) {
                            const sideKey = entree.friesExchange.name;
                            sideCounts[sideKey] = (sideCounts[sideKey] || 0) + (item.quantity || 1);
                          }
                        });
                      }
                      // Check if item includes bread
                      if (item.customization?.sideDish?.toLowerCase().includes('bread') || 
                          item.menuItemName?.toLowerCase().includes('bread')) {
                        breadCount.count += (item.quantity || 1);
                      }
                    });
                    
                    return (
                      <>
                        {Object.entries(entreeCounts).map(([type, count]) => (
                          <div key={type} className="summary-line">
                            <strong>- {count}  {type}</strong>
                          </div>
                        ))}
                        {Object.entries(sideCounts).map(([side, count]) => (
                          <div key={side} className="summary-line">
                            <strong>- {count}  {side}</strong>
                          </div>
                        ))}
                        {breadCount.count > 0 && (
                          <div className="summary-line">
                            <strong>- {breadCount.count}  Bread</strong>
                          </div>
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
                <h4 className="section-title">&gt;&gt; KITCHEN SUMMARY &lt;&lt;</h4>
                <div className="kitchen-summary">
                  {(() => {
                    // Count ALL entrees across all members
                    const entreeCounts: Record<string, number> = {};
                    const sideCounts: Record<string, number> = {};
                    const breadCount = { count: 0 };
                    
                    order.members.forEach((member: any) => {
                      member.cartItems?.forEach((item: any) => {
                        if (item.customization?.entrees) {
                          item.customization.entrees.forEach((entree: any) => {
                            const key = entree.boneType || entree.name || 'Wings';
                            entreeCounts[key] = (entreeCounts[key] || 0) + (item.quantity || 1);
                            
                            if (entree.friesExchange) {
                              const sideKey = entree.friesExchange.name;
                              sideCounts[sideKey] = (sideCounts[sideKey] || 0) + (item.quantity || 1);
                            }
                          });
                        }
                        if (item.customization?.sideDish?.toLowerCase().includes('bread') || 
                            item.menuItemName?.toLowerCase().includes('bread')) {
                          breadCount.count += (item.quantity || 1);
                        }
                      });
                    });
                    
                    return (
                      <>
                        {Object.entries(entreeCounts).map(([type, count]) => (
                          <div key={type} className="summary-line">
                            <strong>- {count}  {type}</strong>
                          </div>
                        ))}
                        {Object.entries(sideCounts).map(([side, count]) => (
                          <div key={side} className="summary-line">
                            <strong>- {count}  {side}</strong>
                          </div>
                        ))}
                        {breadCount.count > 0 && (
                          <div className="summary-line">
                            <strong>- {breadCount.count}  Bread</strong>
                          </div>
                        )}
                      </>
                    );
                  })()}
                </div>
                <div className="divider-line">========================================</div>
                <div className="total-summary">
                  <div>TOTAL ITEMS: {order.items?.reduce((sum: number, item: any) => sum + (item.quantity || 1), 0)}</div>
                  <div>TOTAL PAID:  ${(order.total || order.totalAmount || 0).toFixed(2)}</div>
                </div>
                <div className="divider-line">========================================</div>
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
              <div className="divider-line">----------------------------------------</div>
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
              <div className="divider-line">----------------------------------------</div>
              <div className="total-row grand-total">
                <span className="label">TOTAL MYR</span>
                <span className="value">${(order.total || order.totalAmount || 0).toFixed(2)}</span>
              </div>
              <div className="divider-line">----------------------------------------</div>
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
          <Button color="primary" size="lg" onClick={handlePrint}>
            <i className="bi bi-printer me-2"></i>
            Print Receipt
          </Button>
          <Button color="secondary" size="lg" onClick={toggle} outline>
            Close
          </Button>
        </div>
      </ModalBody>
    </Modal>
  );
};

export default ReceiptModal;
