Markdown

# WingZone Meru - Modernized Receipt & Label System

## System Context
**Establishment:** WingZone Meru (Managed by Zenith Certification Sdn Bhd)
**Address:** Lebuh Meru Raya, Bandar Meru Raya, Ipoh, Perak
**Reg No:** 199401032195 (317877-X)
**Tech Stack:** React (Frontend), Firestore (Database), Thermal Printer Output

---

## 1. Single Order Templates
*Use these templates for individual walk-ins or single-user web orders. Layout is "cleaned up" for better readability.*

### A. Single Order - Dine-In Receipt
*Features: Clean alignment, clear separation of modifiers, "Service Charge" removed (as per current practice).*

```text
========================================
           ** DINE-IN **
           TABLE: 12
========================================

      Zenith Certification Sdn Bhd
        Reg: 199401032195 (317877-X)
          Lebuh Meru Raya,
        Bandar Meru Raya, Ipoh

Date: {{current_date}}    Time: {{time}}
Ord#: {{order_id}}        
----------------------------------------
QTY  ITEM                         PRICE
----------------------------------------

1    CHSBURGER 1/4                23.90
       > Sweet Bombom
       > American Slice
       > Sandwich Prep

1    REG FRIES (COMBO)             0.00

1    COKE ZERO                     3.50
       > No Ice

----------------------------------------
SUBTOTAL                          27.40
SST (6%)                           1.64
----------------------------------------
TOTAL MYR                        $29.04
----------------------------------------
Pay Method: E-Wallet (TnG)

       Wifi: wingzone123
    IG/FB: Wing Zone Malaysia
========================================
B. Single Order - Carry-Out (Pickup) Receipt
Features: Prominent Customer Name for easy pickup calling.

Plaintext

========================================
          ** CARRY-OUT **
       CUST: {{customer_name}}
========================================
     [...Header same as above...]
----------------------------------------
Date: {{current_date}}    Time: {{time}}
Ord#: {{order_id}}      
----------------------------------------

1    10 PC BASKET                 30.50
       > 10 Original (Breaded)
       > Flavor: Honey Q
       > Dip: Ranch
       > Side: Reg Fries

----------------------------------------
SUBTOTAL                          30.50
SST (6%)                           1.83
----------------------------------------
TOTAL MYR                        $32.33
----------------------------------------
        *** P A I D ***
========================================


2. Group Order (Lobby System) Templates
Context: These are unique to your App. They solve the "Messy Distribution" problem.

A. Master Consolidated Invoice (Kitchen Copy)
Features: Separates "Cooking" (Aggregate) from "Packing" (Individual). Tidy columns.

Plaintext

========================================
        ** GROUP LOBBY **
      LOBBY ID: #{{lobby_code}}
      HOST: {{host_name}}
========================================
     [...Header same as above...]
----------------------------------------
Date: {{current_date}}    Time: {{time}}
Status: PAID (Online Gateway)
----------------------------------------

>> KITCHEN PRODUCTION SUMMARY <<
(Cook these items together)

2x   10 PC BASKET
     - 1x Honey Q / Ranch
     - 1x SmokinQ / Blue Cheese

1x   CHSBURGER 1/4
     - Sweet Bombom / Fries

----------------------------------------
TOTAL ITEMS: 3
TOTAL PAID:  $84.90
----------------------------------------

>> PACKING DISTRIBUTION LIST <<
(Peel stickers for these boxes)

[BOX 1] - MAL
   1x CHSBURGER 1/4 (Sweet Bombom)

[BOX 2] - AIRI
   1x 10 PC BASKET (Honey Q)

[BOX 3] - JOHN
   1x 10 PC BASKET (SmokinQ)

========================================
      AUTH CODE: {{auth_id}}
========================================


B. Individual Box Sticker (Thermal Label 2x3")
Features: High-Contrast Layout. The USER NAME is the biggest element to speed up distribution.

Plaintext

+--------------------------------------+
| WINGZONE MERU      GRP: #{{lobby}}   |
+--------------------------------------+
| USER:                                |
| # MAL                                |
+--------------------------------------+
| MEAL:                                |
| 1x CHSBURGER 1/4                     |
|                                      |
|    > FLAVOR: Sweet Bombom            |
|    > ADD-ON: American Slice          |
|    > SIDE:   Reg Fries               |
+--------------------------------------+
| [ ] DRINK: Coke Zero                 |
+--------------------------------------+
| BOX 1 of 3           ORD #{{ord_id}} |
+--------------------------------------+
(Note: For the Wing Basket Sticker)

Plaintext

+--------------------------------------+
| WINGZONE MERU      GRP: #{{lobby}}   |
+--------------------------------------+
| USER:                                |
| # AIRI                               |
+--------------------------------------+
| MEAL:                                |
| 1x 10 PC BASKET                      |
|                                      |
|    > PREP:   Original (Breaded)      |
|    > FLAVOR: Honey Q                 |
|    > DIP:    Ranch                   |
+--------------------------------------+
| [ ] DRINK: Iced Lemon Tea            |
+--------------------------------------+
| BOX 2 of 3           ORD #{{ord_id}} |
+--------------------------------------+


3. Data Structure Reference
Use this structure when mapping Firestore data to the printer logic.

TypeScript

interface ReceiptData {
  header: {
    merchantName: "Zenith Certification Sdn Bhd";
    regNo: "199401032195 (317877-X)";
    address: string;
  };
  
  meta: {
    orderType: "DINE_IN" | "CARRY_OUT" | "GROUP_LOBBY";
    hostName?: string; // Only for Group
    lobbyId?: string;  // Only for Group
    dateTime: string;
  };

  // For the Master Receipt
  consolidatedItems: {
    name: string;
    qty: number;
    price: number;
    modifiers: string[];
  }[];

  // For Stickers & Packing List
  individualDistribution: {
    userName: string; // BIG FONT on sticker
    boxNumber: number;
    totalBoxes: number;
    mainItem: string;
    modifiers: string[];
    drinkChecklist: string; // Bottom checkbox
  }[];
}
```