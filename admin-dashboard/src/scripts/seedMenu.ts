import { collection, addDoc, getDocs, deleteDoc, Timestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';

const FLAVORS = [
  'Buffalo Wing',
  'Sriracha Hot Chilli',
  'Soul of Seoul',
  'Garlic Parm',
  'Mambo Sauce',
  'Sweet Samurai',
  'Honey Q',
  'Blackened Voodoo',
  'Lemon Pepper',
  'Louisiana Smoked',
  'Spicy Alabama',
  'Tokyo Dragon',
  'Thai Chili',
  'Sweet Bombom',
  'Smokin Q'
];

const BEVERAGES = [
  'Coca-Cola',
  'Coke Zero',
  'Sprite',
  'Iced Lemon Tea',
  'Orange Juice'
];

const DIPPING_SAUCES = ['Ranch', 'Bleu Cheese'];

const FRIES_EXCHANGES = [
  { name: 'Premium Wedge Fries', regularPrice: 0, jumboPrice: 8.00 },
  { name: 'Kettle Chips', regularPrice: 0, jumboPrice: 8.00 },
  { name: 'Smiley Fries', regularPrice: 0, jumboPrice: null },
  { name: 'Rice with Grilled Vege', regularPrice: 0, jumboPrice: null },
  { name: 'Flavor Rub Fries', regularPrice: 5.00, jumboPrice: 10.00 },
  { name: 'Sweet Potato Fries', regularPrice: 5.00, jumboPrice: 12.00 },
  { name: 'Mozzarella Stix', regularPrice: 11.00, jumboPrice: null },
  { name: 'Caesar Salad', regularPrice: 14.00, jumboPrice: null },
  { name: 'Garden Salad', regularPrice: 14.00, jumboPrice: null }
];

const menuItems = [
  // ====================
  // COMBO MEALS
  // ====================
  
  {
    name: 'Entree 1',
    description: '6 pcs Boneless Wings + Fries + Fresh Veg + Dipping Sauce + Drink',
    price: 25.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1608039755401-742074f0548d?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBeverage: true,
      requiresDippingSauce: true,
      allowFriesExchange: true,
      allowedFlavors: 1,
      beverages: BEVERAGES,
      dippingSauces: DIPPING_SAUCES,
      friesExchanges: FRIES_EXCHANGES
    }
  },
  {
    name: 'Entree 2',
    description: '7 pcs Original Wings + Fries + Fresh Veg + Dipping Sauce + Drink',
    price: 29.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBeverage: true,
      requiresDippingSauce: true,
      allowFriesExchange: true,
      allowedFlavors: 1,
      beverages: BEVERAGES,
      dippingSauces: DIPPING_SAUCES,
      friesExchanges: FRIES_EXCHANGES
    }
  },
  {
    name: 'Entree 3',
    description: '3 pcs Chicken Tenders + Fries + Fresh Veg + Drink',
    price: 25.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1562967914-608f82629710?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBeverage: true,
      allowFriesExchange: true,
      allowedFlavors: 1,
      beverages: BEVERAGES,
      friesExchanges: FRIES_EXCHANGES
    }
  },
  {
    name: 'Entree 7',
    description: '2 pcs Chicken Tenders + Smiley Fries + Drink (Kid\'s Meal)',
    price: 14.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1619221882018-04f572f5aa25?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBeverage: true,
      allowedFlavors: 1,
      beverages: BEVERAGES
    }
  },
  {
    name: 'Entree 8',
    description: '2 pcs Drumsticks + Fries + Fresh Veg + Dipping Sauce + Drink',
    price: 24.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBeverage: true,
      requiresDippingSauce: true,
      allowFriesExchange: true,
      allowedFlavors: 1,
      beverages: BEVERAGES,
      dippingSauces: DIPPING_SAUCES,
      friesExchanges: FRIES_EXCHANGES
    }
  },

  {
    name: 'Entree 4',
    description: 'Premium Beef Cheeseburger + Fries + Drink',
    price: 27.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      requiresBeverage: true,
      allowFriesExchange: true,
      beverages: BEVERAGES,
      friesExchanges: FRIES_EXCHANGES
    }
  },
  {
    name: 'Entree 5',
    description: 'Supreme Grilled Chicken Sandwich + Fries + Drink',
    price: 27.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1553979459-d2229ba7433b?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      requiresBeverage: true,
      allowFriesExchange: true,
      beverages: BEVERAGES,
      friesExchanges: FRIES_EXCHANGES
    }
  },
  {
    name: 'Entree 6',
    description: 'Supreme Chicken Tender Sandwich + Fries + Drink',
    price: 27.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1619740455993-9e2e38c2f5f7?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      requiresBeverage: true,
      allowFriesExchange: true,
      beverages: BEVERAGES,
      friesExchanges: FRIES_EXCHANGES
    }
  },

  {
    name: 'Entree 10',
    description: 'Supreme Grilled Chicken Tortilla + Kettle Chips + Drink',
    price: 27.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      requiresBeverage: true,
      beverages: BEVERAGES
    }
  },
  {
    name: 'Entree 11',
    description: 'Supreme Chicken Tender Tortilla + Kettle Chips + Drink',
    price: 27.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1604467794349-0b74285de7e7?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      requiresBeverage: true,
      beverages: BEVERAGES
    }
  },
  {
    name: 'Entree 12',
    description: 'Premium Beef Tortilla + Kettle Chips + Drink',
    price: 27.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      requiresBeverage: true,
      beverages: BEVERAGES
    }
  },

  {
    name: 'Entree 9',
    description: 'Garden OR Caesar Salad + Fries + Fresh Veg + Drink',
    price: 22.90,
    category: 'Combo Meals',
    imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      requiresBeverage: true,
      allowFriesExchange: true,
      beverages: BEVERAGES,
      friesExchanges: FRIES_EXCHANGES,
      saladType: ['Garden Salad', 'Caesar Salad']
    }
  },

  // ====================
  // WINGS (A LA CARTE)
  // ====================
  
  {
    name: 'Wings - 5 pcs',
    description: 'Original or Boneless • 1 flavor',
    price: 18.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 1,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings - 7 pcs',
    description: 'Original or Boneless • 1 flavor',
    price: 22.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 1,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings - 10 pcs',
    description: 'Original or Boneless • 1 flavor',
    price: 28.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 1,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings - 15 pcs',
    description: 'Original or Boneless • 2 flavors',
    price: 40.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 2,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings - 20 pcs',
    description: 'Original or Boneless • 2 flavors',
    price: 52.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 2,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings - 30 pcs',
    description: 'Original or Boneless • 2 flavors',
    price: 72.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 2,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings - 50 pcs',
    description: 'Original or Boneless • 3 flavors',
    price: 113.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 3,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings - 100 pcs',
    description: 'Original or Boneless • 4 flavors',
    price: 199.90,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1527477396000-e27163b481c2?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 4,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },

  {
    name: 'Wings + Fries - 7 pcs',
    description: 'Original or Boneless + Premium Wedge Fries • 1 flavor',
    price: 24.50,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1608039755401-742074f0548d?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 1,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },
  {
    name: 'Wings + Fries - 10 pcs',
    description: 'Original or Boneless + Premium Wedge Fries • 1 flavor',
    price: 30.50,
    category: 'Wings',
    imageUrl: 'https://images.unsplash.com/photo-1608039755401-742074f0548d?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      requiresBoneType: true,
      allowedFlavors: 1,
      availableBoneTypes: ['Original', 'Boneless']
    }
  },

  // ====================
  // TENDERS (A LA CARTE)
  // ====================
  
  {
    name: 'Tenders - 3 pcs',
    description: 'A La Carte • 1 flavor',
    price: 16.90,
    category: 'Tenders',
    imageUrl: 'https://images.unsplash.com/photo-1562967914-608f82629710?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Tenders + Fries - 3 pcs',
    description: 'Served with Premium Wedge Fries • 1 flavor',
    price: 21.90,
    category: 'Tenders',
    imageUrl: 'https://images.unsplash.com/photo-1562967914-608f82629710?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Tenders - 5 pcs',
    description: 'A La Carte • 1 flavor',
    price: 23.90,
    category: 'Tenders',
    imageUrl: 'https://images.unsplash.com/photo-1562967914-608f82629710?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Tenders + Fries - 5 pcs',
    description: 'Served with Premium Wedge Fries • 1 flavor',
    price: 27.90,
    category: 'Tenders',
    imageUrl: 'https://images.unsplash.com/photo-1562967914-608f82629710?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Chicken Tenders - 10 pcs',
    description: 'A La Carte • 2 flavors',
    price: 40.90,
    category: 'Chicken Tenders',
    imageUrl: 'https://images.unsplash.com/photo-1562967914-608f82629710?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 2
    }
  },

  // ====================
  // BURGERS & SANDWICHES (A LA CARTE)
  // ====================
  
  {
    name: 'Double Cheeseburger + Fries',
    description: 'Double Stack Premium Beef with Fries',
    price: 29.90,
    category: 'Burgers & Sandwiches',
    imageUrl: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Cheeseburger',
    description: 'Premium Beef Cheeseburger',
    price: 23.90,
    category: 'Burgers & Sandwiches',
    imageUrl: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Beef Tortilla Wrap',
    description: 'Premium Beef in Tortilla',
    price: 23.90,
    category: 'Burgers & Sandwiches',
    imageUrl: 'https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Grilled Chicken Sandwich',
    description: 'Supreme Grilled Chicken',
    price: 23.90,
    category: 'Burgers & Sandwiches',
    imageUrl: 'https://images.unsplash.com/photo-1553979459-d2229ba7433b?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Grilled Chicken Tortilla Wrap',
    description: 'Supreme Grilled Chicken in Tortilla',
    price: 23.90,
    category: 'Burgers & Sandwiches',
    imageUrl: 'https://images.unsplash.com/photo-1626700051175-6818013e1d4f?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Chicken Tender Sandwich',
    description: 'Supreme Chicken Tenders',
    price: 23.90,
    category: 'Burgers & Sandwiches',
    imageUrl: 'https://images.unsplash.com/photo-1619740455993-9e2e38c2f5f7?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Chicken Tender Tortilla Wrap',
    description: 'Supreme Chicken Tenders in Tortilla',
    price: 23.90,
    category: 'Burgers & Sandwiches',
    imageUrl: 'https://images.unsplash.com/photo-1604467794349-0b74285de7e7?w=400',
    isAvailable: true,
    requiresCustomization: false
  },

  // ====================
  // LOCAL FAVORITES
  // ====================
  
  {
    name: 'Flavorholic Boneless',
    description: 'Boneless with Aromatic Rice & Grilled Veg',
    price: 14.90,
    category: 'Local Favorites',
    imageUrl: 'https://images.unsplash.com/photo-1603360946369-dc9bb6258143?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Flavorholic Drums',
    description: 'Drumsticks with Aromatic Rice & Grilled Veg',
    price: 20.90,
    category: 'Local Favorites',
    imageUrl: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Flavorholic Nasi Ayam',
    description: 'Chicken with Rice & Grilled Veg',
    price: 20.90,
    category: 'Local Favorites',
    imageUrl: 'https://images.unsplash.com/photo-1603360946369-dc9bb6258143?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Drumsticks - 2 pcs',
    description: 'Flavorful Drumsticks',
    price: 16.90,
    category: 'Local Favorites',
    imageUrl: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Drumsticks - 3 pcs',
    description: 'Flavorful Drumsticks',
    price: 21.90,
    category: 'Local Favorites',
    imageUrl: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Drumsticks - 5 pcs',
    description: 'Flavorful Drumsticks',
    price: 32.90,
    category: 'Local Favorites',
    imageUrl: 'https://images.unsplash.com/photo-1598103442097-8b74394b95c6?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },

  // ====================
  // SALADS
  // ====================
  
  {
    name: 'Garden Salad',
    description: 'Fresh garden salad',
    price: 18.90,
    category: 'Salads',
    imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Caesar Salad',
    description: 'Classic caesar salad',
    price: 18.90,
    category: 'Salads',
    imageUrl: 'https://images.unsplash.com/photo-1546793665-c74683f339c1?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Garden Salad with Grilled Chicken',
    description: 'Garden salad topped with grilled chicken',
    price: 25.90,
    category: 'Salads',
    imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Caesar Salad with Grilled Chicken',
    description: 'Caesar salad topped with grilled chicken',
    price: 25.90,
    category: 'Salads',
    imageUrl: 'https://images.unsplash.com/photo-1546793665-c74683f339c1?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Garden Salad with Chicken Tender',
    description: 'Garden salad topped with chicken tenders',
    price: 25.90,
    category: 'Salads',
    imageUrl: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Caesar Salad with Chicken Tender',
    description: 'Caesar salad topped with chicken tenders',
    price: 25.90,
    category: 'Salads',
    imageUrl: 'https://images.unsplash.com/photo-1546793665-c74683f339c1?w=400',
    isAvailable: true,
    requiresCustomization: false
  },

  // ====================
  // SIDES
  // ====================
  
  {
    name: 'Wedge Fries - Regular',
    description: 'Premium wedge fries',
    price: 10.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Wedge Fries - Jumbo',
    description: 'Premium wedge fries - Jumbo size',
    price: 16.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Sweet Potato Fries - Regular',
    description: 'Crispy sweet potato fries',
    price: 15.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Sweet Potato Fries - Jumbo',
    description: 'Crispy sweet potato fries - Jumbo size',
    price: 24.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1606755962773-d324e0a13086?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Kettle Chips - Regular',
    description: 'Crunchy kettle chips',
    price: 10.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Kettle Chips - Jumbo',
    description: 'Crunchy kettle chips - Jumbo size',
    price: 16.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Flavor Rub Fries - Regular',
    description: 'Fries with your choice of flavor rub',
    price: 13.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Flavor Rub Fries - Jumbo',
    description: 'Fries with your choice of flavor rub - Jumbo size',
    price: 19.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },
  {
    name: 'Mozzarella Stix',
    description: 'Crispy mozzarella sticks',
    price: 20.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1531749668029-2db88e4276c7?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Smiley Fries',
    description: 'Fun smiley face fries',
    price: 9.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1598679253544-2c97992403ea?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Aromatic Rice',
    description: 'Fragrant aromatic rice',
    price: 4.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1516684732162-798a0062be99?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Aromatic Rice with Grilled Veg',
    description: 'Fragrant aromatic rice with grilled vegetables',
    price: 6.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1516684732162-798a0062be99?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Celeries',
    description: 'Fresh celery sticks',
    price: 4.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1584270354949-c26b0d5b4a0c?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Ranch or Bleu Cheese',
    description: 'Choice of ranch or bleu cheese dressing',
    price: 4.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1472476443507-c7a5948772fc?w=400',
    isAvailable: true,
    requiresCustomization: true,
    customizationOptions: {
      dressingType: ['Ranch', 'Bleu Cheese']
    }
  },
  {
    name: 'Extra Flavors',
    description: 'Additional flavor sauce',
    price: 4.90,
    category: 'Sides',
    imageUrl: 'https://images.unsplash.com/photo-1472476443507-c7a5948772fc?w=400',
    isAvailable: true,
    requiresCustomization: true,
    flavors: FLAVORS,
    customizationOptions: {
      requiresFlavor: true,
      allowedFlavors: 1
    }
  },

  // ====================
  // BEVERAGES
  // ====================
  
  {
    name: 'Coca-Cola',
    description: 'Classic Coca-Cola',
    price: 5.90,
    category: 'Beverages',
    imageUrl: 'https://images.unsplash.com/photo-1554866585-cd94860890b7?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Coke Zero',
    description: 'Zero sugar Coca-Cola',
    price: 5.90,
    category: 'Beverages',
    imageUrl: 'https://images.unsplash.com/photo-1554866585-cd94860890b7?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Sprite',
    description: 'Refreshing Sprite',
    price: 5.90,
    category: 'Beverages',
    imageUrl: 'https://images.unsplash.com/photo-1625772299848-391b6a87d7b3?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Iced Lemon Tea',
    description: 'Refreshing iced lemon tea',
    price: 5.90,
    category: 'Beverages',
    imageUrl: 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=400',
    isAvailable: true,
    requiresCustomization: false
  },
  {
    name: 'Orange Juice',
    description: 'Fresh orange juice',
    price: 7.90,
    category: 'Beverages',
    imageUrl: 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=400',
    isAvailable: true,
    requiresCustomization: false
  }
];

export async function seedMenuItems() {
  try {
    console.log('🌱 Starting menu seed...');

    // Clear existing menu items
    console.log('🗑️  Clearing existing menu items...');
    const existingItems = await getDocs(collection(db, 'menuItems'));
    const deletePromises = existingItems.docs.map(doc => deleteDoc(doc.ref));
    await Promise.all(deletePromises);
    console.log(`✅ Deleted ${existingItems.size} existing items`);

    // Add new menu items
    console.log('📝 Adding new menu items...');
    const addPromises = menuItems.map(item => 
      addDoc(collection(db, 'menuItems'), {
        ...item,
        createdAt: Timestamp.now(),
        updatedAt: Timestamp.now()
      })
    );
    await Promise.all(addPromises);

    console.log(`✅ Successfully added ${menuItems.length} menu items!`);
    console.log('🎉 Menu seed completed!');
    
    return { success: true, count: menuItems.length };
  } catch (error) {
    console.error('❌ Error seeding menu:', error);
    throw error;
  }
}
