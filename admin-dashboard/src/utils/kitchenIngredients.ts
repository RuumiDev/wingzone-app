// Shared utility for aggregating kitchen ingredients from order items

export interface OrderItem {
  name: string;
  quantity: number;
  customizations?: any;
  customization?: any;  // Support both naming conventions
  menuItemId?: string;
}

export function aggregateKitchenIngredients(items: OrderItem[], menuItems?: any[]): Record<string, number> {
  const kitchenIngredientsSummary: Record<string, number> = {};
  
  items.forEach((item) => {
    // Support both customizations and customization field names
    const customization = item.customizations || item.customization || {};
    const flavor = customization.flavor || '';
    const boneType = customization.boneType || '';
    const friesExchange = customization.friesExchange;
    const saladType = customization.saladType;
    
    // Look up menu item to get kitchen ingredients
    const menuItem = menuItems?.find(mi => 
      mi.id === item.menuItemId || mi.name === item.name
    );
    
    if (menuItem?.kitchenIngredients?.ingredients) {
      // Use defined kitchen ingredients from menu item
      menuItem.kitchenIngredients.ingredients.forEach((ingredient: any) => {
        let key = '';
        let qty = ingredient.quantity * item.quantity;
        
        // Handle different ingredient types
        if (ingredient.type === 'wings' && ingredient.requiresSelection) {
          // Wings require flavor/bone type
          key = `${boneType || 'Original'} Wings - ${flavor || 'Plain'}`;
        } else if (ingredient.type === 'boneless' && ingredient.requiresSelection) {
          // Boneless wings with flavor
          key = `Boneless Wings - ${flavor || 'Plain'}`;
        } else if (ingredient.type === 'tenders' && ingredient.requiresSelection) {
          // Tenders with flavor
          key = `Chicken Tenders - ${flavor || 'Plain'}`;
        } else if (ingredient.type === 'fries') {
          // Check if customer swapped fries
          if (friesExchange) {
            key = friesExchange.name;
          } else {
            key = 'Reg Fries';  // Default
          }
        } else if (ingredient.type === 'wedge_fries') {
          // Check if customer swapped wedge fries
          if (friesExchange) {
            key = friesExchange.name;
          } else {
            key = 'Wedge Fries';
          }
        } else if (ingredient.type === 'salad') {
          // Use customer's salad choice
          key = saladType || 'Garden Salad';
        } else if (ingredient.type === 'bread') {
          key = 'Bread';
        } else if (ingredient.type === 'grilled_vegetables') {
          key = 'Grilled Vegetables';
        } else {
          // Generic ingredient (e.g., "rice", "cheese", "burger_bun")
          key = ingredient.type
            .replace(/_/g, ' ')
            .replace(/\b\w/g, (c: string) => c.toUpperCase());
        }
        
        if (key) {
          kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + qty;
        }
      });
    } else {
      // Fallback: Parse from item name if no kitchen ingredients defined
      const itemName = item.name.toLowerCase();
      
      // Extract wing quantities from item names (legacy logic)
      if (itemName.includes('entree 1')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (6 * item.quantity);
      } else if (itemName.includes('entree 2')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (7 * item.quantity);
        // Default sides for Entree 2
        if (friesExchange) {
          kitchenIngredientsSummary[friesExchange.name] = (kitchenIngredientsSummary[friesExchange.name] || 0) + item.quantity;
        } else {
          kitchenIngredientsSummary['Reg Fries'] = (kitchenIngredientsSummary['Reg Fries'] || 0) + item.quantity;
        }
        kitchenIngredientsSummary['Bread'] = (kitchenIngredientsSummary['Bread'] || 0) + item.quantity;
      } else if (itemName.includes('entree 3') || itemName.includes('entree 4')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (8 * item.quantity);
      } else if (itemName.includes('entree 5') || itemName.includes('entree 6')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (10 * item.quantity);
      } else if (itemName.includes('entree 7') || itemName.includes('entree 8')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (15 * item.quantity);
      } else if (itemName.includes('wings - 5')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (5 * item.quantity);
      } else if (itemName.includes('wings - 10')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (10 * item.quantity);
      } else if (itemName.includes('wings - 15')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (15 * item.quantity);
      } else if (itemName.includes('wings - 20')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (20 * item.quantity);
      } else if (itemName.includes('wings - 50')) {
        const key = `${boneType} Wings - ${flavor}`;
        kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (50 * item.quantity);
      }
      
      // Add tenders
      if (itemName.includes('tender') && !itemName.includes('salad')) {
        const tenderFlavor = customization.flavor || 'Plain';
        const key = `Chicken Tenders - ${tenderFlavor}`;
        
        if (itemName.includes('3 pcs')) {
          kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (3 * item.quantity);
        } else if (itemName.includes('5 pcs')) {
          kitchenIngredientsSummary[key] = (kitchenIngredientsSummary[key] || 0) + (5 * item.quantity);
        }
      }
    }
  });
  
  return kitchenIngredientsSummary;
}
