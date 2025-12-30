import React from 'react';
import { Modal, ModalHeader, ModalBody, Button, Row, Col } from 'reactstrap';
import './PackagingStickerModal.scss';

interface PackagingStickerModalProps {
  isOpen: boolean;
  toggle: () => void;
  order: any;
}

const PackagingStickerModal: React.FC<PackagingStickerModalProps> = ({ isOpen, toggle, order }) => {
  const handlePrintStickers = () => {
    window.print();
  };

  if (!order || !order.lobbyId || !order.members) return null;

  // Generate stickers for each member's items
  const generateStickers = () => {
    const stickers: any[] = [];
    let stickerIndex = 1;
    const totalStickers = order.members.reduce(
      (sum: number, member: any) => sum + (member.cartItems?.length || 0),
      0
    );

    order.members.forEach((member: any) => {
      member.cartItems?.forEach((item: any) => {
        stickers.push({
          boxNumber: stickerIndex,
          totalBoxes: totalStickers,
          userName: member.name,
          item: item,
          lobbyCode: order.code,
          orderId: order.id
        });
        stickerIndex++;
      });
    });

    return stickers;
  };

  const stickers = generateStickers();

  return (
    <Modal isOpen={isOpen} toggle={toggle} size="xl" className="sticker-modal">
      <ModalHeader toggle={toggle}>
        <i className="bi bi-tag me-2"></i>
        Packaging Stickers - Group Order #{order.code}
      </ModalHeader>
      <ModalBody>
        <div className="stickers-container" id="stickers-print">
          <Row>
            {stickers.map((sticker, index) => (
              <Col key={index} md={6} className="sticker-col">
                <div className="packaging-sticker">
                  {/* Header */}
                  <div className="sticker-header">
                    <span className="brand">WINGZONE MERU</span>
                    <span className="group-id">GRP: #{sticker.lobbyCode}</span>
                  </div>

                  {/* User Name Section */}
                  <div className="user-section">
                    <div className="user-label">USER:</div>
                    <div className="user-name"># {sticker.userName.toUpperCase()}</div>
                  </div>

                  {/* Meal Details */}
                  <div className="item-section">
                    <div className="item-label">MEAL:</div>
                    <div className="item-name">
                      {sticker.item.quantity}x {sticker.item.menuItemName || sticker.item.name}
                    </div>
                    
                    {sticker.item.customization && (
                      <div className="item-details">
                        {sticker.item.customization.boneType && (
                          <div className="detail-row">
                            <span className="detail-label">   &gt; PREP:</span>
                            <span className="detail-value">{sticker.item.customization.boneType}</span>
                          </div>
                        )}
                        {sticker.item.customization.flavor && (
                          <div className="detail-row">
                            <span className="detail-label">   &gt; FLAVOR:</span>
                            <span className="detail-value">{sticker.item.customization.flavor}</span>
                          </div>
                        )}
                        {sticker.item.customization.dippingSauce && (
                          <div className="detail-row">
                            <span className="detail-label">   &gt; DIP:</span>
                            <span className="detail-value">{sticker.item.customization.dippingSauce}</span>
                          </div>
                        )}
                        {sticker.item.customization.sideDish && (
                          <div className="detail-row">
                            <span className="detail-label">   &gt; SIDE:</span>
                            <span className="detail-value">{sticker.item.customization.sideDish}</span>
                          </div>
                        )}
                        {sticker.item.customization.addOn && (
                          <div className="detail-row">
                            <span className="detail-label">   &gt; ADD-ON:</span>
                            <span className="detail-value">{sticker.item.customization.addOn}</span>
                          </div>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Drink Checkbox */}
                  {sticker.item.customization?.drink && (
                    <div className="drink-section">
                      <input type="checkbox" className="drink-checkbox" />
                      <span className="drink-label">DRINK: {sticker.item.customization.drink}</span>
                    </div>
                  )}

                  {/* Footer */}
                  <div className="sticker-footer">
                    <div className="footer-info">
                      <span>BOX {sticker.boxNumber} of {sticker.totalBoxes}</span>
                      <span>ORD #{sticker.orderId.substring(0, 8).toUpperCase()}</span>
                    </div>
                  </div>
                </div>
              </Col>
            ))}
          </Row>
        </div>

        <div className="sticker-actions print-hide">
          <Button color="primary" size="lg" onClick={handlePrintStickers}>
            <i className="bi bi-printer me-2"></i>
            Print All Stickers ({stickers.length})
          </Button>
          <Button color="secondary" size="lg" onClick={toggle} outline>
            Close
          </Button>
        </div>
      </ModalBody>
    </Modal>
  );
};

export default PackagingStickerModal;
