# UI/UX Redesign - ZUS Coffee Style

## Overview
Complete redesign of WingZone app layouts inspired by ZUS Coffee's modern, clean design patterns.

**Date:** January 23, 2025  
**Reference:** ZUS Coffee Malaysia app design

---

## 🎨 Design Philosophy

### Color Palette (ZUS-Inspired)
- **Primary Navy Blue**: `#1E3A8A` (buttons, headers, selected states)
- **Background Gray**: `#F9FAFB` (screen backgrounds)
- **Border Gray**: `#E5E7EB` (dividers, chip borders)
- **Text Primary**: `#1F2937` (headings, labels)
- **Text Secondary**: `#6B7280` (descriptions, hints)
- **White**: `#FFFFFF` (cards, surfaces)
- **WingZone Red**: Kept for branding (badges, accents)

### Key Design Patterns
1. **Grid Layout**: 2-column product grid vs old list view
2. **Chip Selections**: Rounded chip buttons for customizations
3. **Bottom Sheets**: Modal bottom sheets for cart
4. **Section Cards**: White cards with rounded corners on gray background
5. **Radio Button Groups**: For order type selection (Dine-in/Pickup)
6. **Full-Screen Dialogs**: For detailed item customization

---

## 📱 New Screens Created

### 1. MenuScreenNew.kt
**Location:** `app/src/main/java/wingzone/zenith/ui/screens/MenuScreenNew.kt`

**Features:**
- **Category Sidebar** (100dp width)
  - Vertical scrollable category tabs
  - Icon + text for each category
  - Selected state with blue highlight
  - Replaces horizontal chip row

- **Grid Layout**
  - 2-column grid using `LazyVerticalGrid`
  - Product cards with 0.75 aspect ratio
  - Large product image area
  - Product name + price below image
  - Clean, spacious design

- **Floating Cart Badge**
  - FAB with cart icon + item count badge
  - Shows only when cart has items
  - Opens cart bottom sheet

**Components:**
```kotlin
MenuTopBar() // Top bar with cart icon
CategorySidebar() // Left vertical categories
MenuGrid() // 2-column product grid
MenuItemGridCard() // Individual product card
```

---

### 2. ItemCustomizationScreen.kt
**Location:** `app/src/main/java/wingzone/zenith/ui/screens/ItemCustomizationScreen.kt`

**Features:**
- **Full-Screen Layout**
  - Replaces dialog with full screen
  - Back button in top bar
  - Large hero image (300dp height)
  - Product name + description

- **Chip-Style Selections**
  - Rounded chips for all options
  - Navy blue for selected state
  - White with border for unselected
  - 2-column grid for flavor/drink chips
  - Full-width chips for fries exchange

- **Section Headers**
  - "* Pick 1" for required selections
  - "Optional" for non-required
  - Clear visual hierarchy

- **Bottom Action Bar**
  - Price display (updates with selections)
  - Quantity controls (- / count / +)
  - "Add To Cart" button (enabled when valid)
  - Fixed at bottom, above content

**Components:**
```kotlin
ItemCustomizationScreen() // Main screen
SelectionSection() // Section with title + chips
FlavorChipGroup() // Flavor selection chips
SelectionChip() // Individual chip button
CustomizationBottomBar() // Price + quantity + add button
```

**Layout Structure:**
```
┌─────────────────────────┐
│  ← Back         [Image] │ TopBar
├─────────────────────────┤
│                         │
│   Large Product Image   │ 300dp
│                         │
├─────────────────────────┤
│ Product Name            │
│ Description text...     │
├─────────────────────────┤
│ Choose Flavor  * Pick 1 │
│ [Buffalo] [BBQ]         │
│ [Honey G] [Lemon P]     │
├─────────────────────────┤
│ Choose Dipping Sauce    │
│ [Ranch] [Blue Cheese]   │
├─────────────────────────┤
│ Choose Drink   * Pick 1 │
│ [Coke] [Sprite]         │
│ [Ice Tea] [Orange]      │
├─────────────────────────┤
│ Fries Exchange Optional │
│ [Wedge Fries (FREE)]    │
│ [Sweet Potato (+5)]     │
├─────────────────────────┤
│ RM 24.90    [-] 1 [+]   │
│ [   Add To Cart   ]     │ Fixed Bottom Bar
└─────────────────────────┘
```

---

### 3. CartBottomSheet.kt
**Location:** `app/src/main/java/wingzone/zenith/ui/screens/CartBottomSheet.kt`

**Features:**
- **Modal Bottom Sheet**
  - Slides up from bottom
  - Rounded top corners (20dp)
  - Fills 85% of screen height
  - Dismissible by drag/tap outside

- **Header Section**
  - "My Cart" title
  - "Clear Order" button with delete icon
  - Close (X) button

- **Cart Item Rows**
  - 70dp product image thumbnail
  - Item name + customizations
  - Price + quantity controls
  - Delete button per item
  - Inline quantity adjustment (- / x2 / +)

- **Bottom Checkout Section**
  - Total price display
  - "Checkout" button
  - Fixed at bottom of sheet
  - Elevated shadow

**Components:**
```kotlin
CartBottomSheet() // Main modal sheet
CartSheetHeader() // Header with title + actions
CartItemRow() // Individual cart item
CartCheckoutSection() // Bottom bar with total + button
buildCustomizationText() // Helper for display
```

**Layout:**
```
┌─────────────────────────┐
│ My Cart    [Clear] [X]  │ Header
├─────────────────────────┤
│ [img] Item Name    [Del]│
│       Customizations    │
│       RM 24.90  - x2 +  │
├─────────────────────────┤
│ [img] Item Name    [Del]│
│       Customizations    │
│       RM 14.90  - x1 +  │
├─────────────────────────┤
│                         │ Scrollable
│                         │ Content
│                         │
├─────────────────────────┤
│ Total          RM 39.80 │
│              [Checkout] │ Fixed Bottom
└─────────────────────────┘
```

---

### 4. CheckoutScreen.kt
**Location:** `app/src/main/java/wingzone/zenith/ui/screens/CheckoutScreen.kt`

**Features:**
- **Comprehensive Checkout Flow**
  - Matches ZUS order confirmation design
  - All sections in scrollable LazyColumn
  - White cards on gray background
  - Clean section headers

- **Sections Included:**
  1. **Paper Bag Option**: Checkbox with icon
  2. **Pickup Location**: 
     - Restaurant address with map icon
     - Pickup time
     - Dine-in / Pick Up To Go selection (radio buttons)
  3. **Your Order**: 
     - List of cart items with thumbnails
     - "Add Items" link
     - "EDIT" button per item
  4. **Payment Methods**: 
     - Selectable payment options
     - WZ Balance promotion message
  5. **Vouchers**: 
     - "BUY1FREE1" badge
     - "Add Voucher" button
  6. **Payment Details**: 
     - Amount breakdown
     - SST (6%)
     - Voucher discount
     - Grand Total
     - Points earned
     - Cup count

- **Bottom Bar**
  - Item count + total price
  - "Order Now" button
  - Enabled only when payment method selected

**Components:**
```kotlin
CheckoutScreen() // Main screen
PaperBagOption() // Checkbox selection
PickupLocationSection() // Location + order type
OrderTypeOption() // Radio button chip
YourOrderSection() // Cart items list
CheckoutOrderItem() // Single order item
PaymentMethodsSection() // Payment selection
VouchersSection() // Voucher input
PaymentDetailsSection() // Price breakdown
PaymentDetailRow() // Individual price row
CheckoutBottomBar() // Bottom action bar
```

**Section Layout:**
```
┌─────────────────────────┐
│ ← Order Confirmation    │ TopBar
├─────────────────────────┤
│                         │
│ ☐ I need Paper Bag      │ Option Card
├─────────────────────────┤
│ Pickup At               │ Header
│ ┌─────────────────────┐ │
│ │ 📍 WingZone Ipoh    │ │
│ │ Address details...   │ │
│ │ ⏰ Pickup: ASAP     │ │
│ ├─────────────────────┤ │
│ │ ⚪ Dine-In           │ │ Radio Chips
│ │ ⚪ Pick Up To Go    │ │
│ └─────────────────────┘ │
├─────────────────────────┤
│ Your Order   [Add Items]│ Header
│ ┌─────────────────────┐ │
│ │ [img] Item   [EDIT] │ │
│ │ RM 24.90            │ │
│ └─────────────────────┘ │
├─────────────────────────┤
│ Payment Methods         │
│ ┌─────────────────────┐ │
│ │ Select payment...  >│ │
│ └─────────────────────┘ │
│ 💡 Use WZ Balance!      │ Hint
├─────────────────────────┤
│ Vouchers     [BUY1FREE1]│ Badge
│ ┌─────────────────────┐ │
│ │ Add Voucher       > │ │
│ └─────────────────────┘ │
├─────────────────────────┤
│ Payment Details         │
│ ┌─────────────────────┐ │
│ │ Amount      RM 39.80│ │
│ │ Voucher     - RM 0  │ │
│ │ ─────────────────── │ │
│ │ Grand Total RM 39.80│ │ Bold
│ └─────────────────────┘ │
├─────────────────────────┤
│ 2 items    RM 39.80     │
│              [Order Now]│ Bottom Bar
└─────────────────────────┘
```

---

## 🔄 Comparison: Old vs New

### Menu Screen
| Aspect | Old Design | New Design |
|--------|-----------|------------|
| Layout | List view | **2-column grid** |
| Categories | Horizontal scroll chips | **Vertical sidebar** |
| Product Card | Horizontal row | **Vertical card with image** |
| Image Size | 80dp square | **Full card width × 200dp** |
| Spacing | Tight (12dp) | **Spacious (12-16dp)** |
| Cart Access | Top bar icon | **Floating Action Button** |

### Customization
| Aspect | Old Design | New Design |
|--------|-----------|------------|
| Container | Dialog (90% height) | **Full screen** |
| Product Image | Small in header | **Large hero (300dp)** |
| Selections | List with radio buttons | **Chip buttons** |
| Selection Feedback | Checkmark icon | **Navy blue background** |
| Quantity | Separate section at top | **Bottom bar with price** |
| Submit Button | In footer | **Fixed bottom bar** |

### Cart
| Aspect | Old Design | New Design |
|--------|-----------|------------|
| Display | Full screen | **Bottom sheet modal** |
| Access | Navigation item | **FAB or top bar icon** |
| Clear Cart | Not visible | **"Clear Order" in header** |
| Item Actions | Edit not available | **Inline quantity + delete** |
| Checkout | Fixed bottom bar | **Fixed in sheet bottom** |

### Checkout
| Aspect | Old Design | New Design |
|--------|-----------|------------|
| Sections | Basic order summary | **8 detailed sections** |
| Layout | Simple list | **Card-based sections** |
| Order Type | Not shown | **Radio button selection** |
| Payment | Basic | **Dedicated section + promo** |
| Vouchers | Not included | **Prominent with badge** |
| Details | Minimal | **Full breakdown with SST** |

---

## 🎯 Design Improvements

### Visual Hierarchy
- ✅ Clear section headers (18sp bold)
- ✅ Consistent spacing (16-20dp)
- ✅ Card elevations (2-8dp)
- ✅ Rounded corners (12-16dp)
- ✅ Navy blue for primary actions

### User Experience
- ✅ Grid view shows more products
- ✅ Larger images for better visual appeal
- ✅ Chip selections are faster than dropdowns
- ✅ Bottom sheet cart is non-blocking
- ✅ Inline quantity adjustment is intuitive
- ✅ Full-screen customization focuses attention
- ✅ Checkout breakdown is transparent

### Accessibility
- ✅ Larger touch targets (48dp minimum)
- ✅ High contrast text colors
- ✅ Clear selection states
- ✅ Icon + text labels
- ✅ Descriptive button text

### Performance
- ✅ Grid with `LazyVerticalGrid` (efficient)
- ✅ Bottom sheet only renders when open
- ✅ Image loading with Coil (cached)
- ✅ Reusable composables

---

## 📦 Component Reusability

### Shared Components
```kotlin
// Selection Chips (used across screens)
SelectionChip(text, isSelected, onClick, modifier)

// Section Headers
SelectionSection(title, required, pickCount, content)

// Price Display
PaymentDetailRow(title, value, colors, sizes)

// Quantity Controls
// Consistent [-] count [+] pattern

// Bottom Action Bars
// Consistent price + action button layout
```

---

## 🚀 Implementation Notes

### Dependencies Required
```gradle
// Coil for image loading
implementation("io.coil-kt:coil-compose:2.4.0")

// Already have Material3
implementation("androidx.compose.material3:material3")
```

### Migration Steps
1. **Test New Screens**: 
   - Replace `MenuScreen` with `MenuScreenNew`
   - Replace `EntreeCustomizationDialog` with `ItemCustomizationScreen`
   - Replace cart screen with `CartBottomSheet`
   - Add `CheckoutScreen` to navigation

2. **Navigation Updates**:
   ```kotlin
   // In navigation graph
   composable("menu") { MenuScreenNew(...) }
   composable("checkout") { CheckoutScreen(...) }
   ```

3. **ViewModel Updates**:
   - No changes needed (same data structures)
   - Cart and Menu ViewModels work as-is

4. **Theme Updates**:
   ```kotlin
   // Add ZUS colors to theme
   val NavyBlue = Color(0xFF1E3A8A)
   val LightBlue = Color(0xFFEFF6FF)
   val BorderGray = Color(0xFFE5E7EB)
   ```

---

## 📊 Files Created

### New Files
1. `MenuScreenNew.kt` - Modern grid-based menu (426 lines)
2. `ItemCustomizationScreen.kt` - Full-screen customization (448 lines)
3. `CartBottomSheet.kt` - Bottom sheet cart (295 lines)
4. `CheckoutScreen.kt` - Complete checkout flow (654 lines)

### Total
- **4 new files**
- **1,823 lines of code**
- **~40 reusable composables**

---

## ✅ Testing Checklist

### Menu Screen
- [ ] Categories display in sidebar
- [ ] Products display in 2-column grid
- [ ] Images load correctly
- [ ] Category selection scrolls to items
- [ ] Cart FAB shows when items added
- [ ] Cart badge shows correct count

### Customization Screen
- [ ] Full screen opens on item click
- [ ] Large image displays
- [ ] All selection chips work
- [ ] Selected state shows navy blue
- [ ] Fries exchange shows with pricing
- [ ] Quantity controls update price
- [ ] Add to Cart only enabled when valid
- [ ] Back button dismisses screen

### Cart Bottom Sheet
- [ ] Sheet slides up from bottom
- [ ] Dismisses on outside tap
- [ ] Clear Order empties cart
- [ ] Quantity controls update cart
- [ ] Delete button removes items
- [ ] Total price updates correctly
- [ ] Checkout button navigates

### Checkout Screen
- [ ] All sections display
- [ ] Dine-in/Pickup selection works
- [ ] Payment method selection required
- [ ] Voucher section functional
- [ ] Order items show with images
- [ ] Price breakdown accurate
- [ ] Order Now enabled when valid

---

## 🎨 Design Inspiration

### ZUS Coffee Design Elements Used
✅ **Grid menu layout** - Modern, visual  
✅ **Category sidebar** - Easy navigation  
✅ **Chip selections** - Fast, touch-friendly  
✅ **Bottom sheet cart** - Non-intrusive  
✅ **Full-screen customization** - Focused experience  
✅ **Navy blue primary color** - Professional  
✅ **Card-based sections** - Clean separation  
✅ **Radio button groups** - Clear choices  
✅ **Inline quantity controls** - Intuitive  
✅ **Transparent pricing** - Build trust  

---

## 📝 Next Steps

1. **Replace old screens** with new implementations
2. **Test on physical devices** (different screen sizes)
3. **Add animations** (sheet slide, chip selection, etc.)
4. **Implement image uploading** in admin dashboard
5. **Add search/filter** to menu grid
6. **Implement payment integration**
7. **Add order tracking** post-checkout

---

**Redesign by:** GitHub Copilot (Claude Sonnet 4.5)  
**Date:** January 23, 2025  
**Reference:** ZUS Coffee Malaysia mobile app
