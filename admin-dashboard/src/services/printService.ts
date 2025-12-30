// Auto-print service for group order receipts
import { notificationService } from './notifications';

export interface GroupOrderParticipant {
  userId: string;
  userName: string;
  items: Array<{
    name: string;
    quantity: number;
    price: number;
    customizations?: any;
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
}

class PrintService {
  private autoPrintEnabled: boolean = true;

  // Set auto-print preference
  setAutoPrintEnabled(enabled: boolean) {
    this.autoPrintEnabled = enabled;
  }

  // Format receipt HTML for a single participant
  private formatParticipantReceipt(
    participant: GroupOrderParticipant,
    groupOrder: GroupOrderForPrint,
    participantNumber: number,
    totalParticipants: number
  ): string {
    const itemsHtml = participant.items
      .map(
        (item) => `
        <tr>
          <td style="padding: 8px 0; border-bottom: 1px dashed #ddd;">
            ${item.quantity}x ${item.name}
          </td>
          <td style="padding: 8px 0; text-align: right; border-bottom: 1px dashed #ddd;">
            RM ${item.price.toFixed(2)}
          </td>
        </tr>
      `
      )
      .join('');

    return `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>Receipt - ${participant.userName}</title>
        <style>
          @media print {
            @page {
              size: 80mm auto;
              margin: 5mm;
            }
            body {
              margin: 0;
              padding: 0;
            }
          }
          body {
            font-family: 'Courier New', monospace;
            width: 80mm;
            margin: 0 auto;
            padding: 10px;
            font-size: 12px;
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
          <div class="logo">🍗 WINGZONE</div>
          <div>Order Receipt</div>
          <div style="font-size: 10px; margin-top: 5px;">
            ${groupOrder.orderDate.toLocaleString()}
          </div>
        </div>

        <div class="group-info">
          <strong>GROUP ORDER</strong><br>
          Group: ${groupOrder.groupName}<br>
          Host: ${groupOrder.hostUserName}<br>
          Order ID: ${groupOrder.id.substring(0, 8).toUpperCase()}
        </div>

        <div class="participant-badge">
          MEMBER ${participantNumber} OF ${totalParticipants}
        </div>

        <div style="margin: 15px 0; font-size: 14px; font-weight: bold;">
          FOR: ${participant.userName.toUpperCase()}
        </div>

        <div class="instruction">
          📦 STAPLE TO PACKAGE
        </div>

        <table>
          <thead>
            <tr style="border-bottom: 2px solid #000;">
              <th style="text-align: left; padding: 8px 0;">Item</th>
              <th style="text-align: right; padding: 8px 0;">Price</th>
            </tr>
          </thead>
          <tbody>
            ${itemsHtml}
          </tbody>
        </table>

        <table>
          <tr class="total-row">
            <td>TOTAL:</td>
            <td style="text-align: right;">RM ${participant.total.toFixed(2)}</td>
          </tr>
        </table>

        <div class="footer">
          <div>Thank you for your order!</div>
          <div>www.wingzone.com.my</div>
          <div style="margin-top: 10px;">
            Member ${participantNumber} of ${totalParticipants} in group
          </div>
        </div>
      </body>
      </html>
    `;
  }

  // Print individual receipts for all participants in a group order
  async printGroupOrderReceipts(groupOrder: GroupOrderForPrint) {
    if (!this.autoPrintEnabled) {
      console.log('Auto-print is disabled');
      return;
    }

    try {
      const totalParticipants = groupOrder.participants.length;

      // Print each participant's receipt
      for (let i = 0; i < groupOrder.participants.length; i++) {
        const participant = groupOrder.participants[i];
        const receiptHtml = this.formatParticipantReceipt(
          participant,
          groupOrder,
          i + 1,
          totalParticipants
        );

        // Create a new window for printing
        const printWindow = window.open('', '_blank', 'width=300,height=600');
        if (printWindow) {
          printWindow.document.write(receiptHtml);
          printWindow.document.close();

          // Wait for content to load, then print
          printWindow.onload = () => {
            setTimeout(() => {
              printWindow.print();
              // Close after printing (user can cancel)
              setTimeout(() => {
                printWindow.close();
              }, 500);
            }, 250);
          };
        }

        // Small delay between prints to avoid browser issues
        await new Promise((resolve) => setTimeout(resolve, 500));
      }

      console.log(`Printed ${totalParticipants} receipts for group order ${groupOrder.id}`);
    } catch (error) {
      console.error('Error printing group order receipts:', error);
    }
  }

  // Manual print for a specific participant
  printSingleParticipantReceipt(
    participant: GroupOrderParticipant,
    groupOrder: GroupOrderForPrint,
    participantNumber: number,
    totalParticipants: number
  ) {
    const receiptHtml = this.formatParticipantReceipt(
      participant,
      groupOrder,
      participantNumber,
      totalParticipants
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
