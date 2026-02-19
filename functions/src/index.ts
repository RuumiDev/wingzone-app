/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

import {setGlobalOptions} from "firebase-functions";
import * as logger from "firebase-functions/logger";
import * as admin from "firebase-admin";
import {onDocumentCreated, onDocumentUpdated} from "firebase-functions/v2/firestore";
import {onRequest} from "firebase-functions/v2/https";

// Initialize Firebase Admin
admin.initializeApp();

// Start writing functions
// https://firebase.google.com/docs/functions/typescript

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

/**
 * Auto-progress individual orders through status workflow:
 * pending -> confirmed (immediately) -> preparing (30s)
 * 
 * After "ready" status is manually set, orders auto-deliver after 5 min
 */
export const autoProgressIndividualOrders = onDocumentCreated(
  "orders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const order = event.data?.data();

    if (!order) {
      logger.warn(`Order ${orderId} has no data`);
      return;
    }

    logger.info(`New order created: ${orderId}, status: ${order.status}, paymentStatus: ${order.paymentStatus}, paymentMethod: ${order.paymentMethod}`);

    // Skip auto-confirm for cash orders awaiting payment confirmation from admin
    if (order.paymentStatus === "unpaid" || order.paymentMethod === "cash") {
      logger.info(`Order ${orderId} is a cash/unpaid order — skipping auto-confirm, waiting for admin payment confirmation`);
      return;
    }

    try {
      // Immediately move to confirmed (online/paid orders only)
      await admin.firestore()
        .collection("orders")
        .doc(orderId)
        .update({
          status: "confirmed",
          confirmedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      logger.info(`Order ${orderId} confirmed immediately`);
    } catch (error) {
      logger.error(`Error confirming order ${orderId}:`, error);
    }
  }
);

/**
 * When order is confirmed, schedule it to move to preparing after 30 seconds
 */
export const onOrderConfirmed = onDocumentUpdated(
  "orders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) return;

    // Check if order just became confirmed
    if (before.status !== "confirmed" && after.status === "confirmed") {
      logger.info(`Order ${orderId} confirmed, will prepare in 30s`);

      try {
        // Wait 30 seconds
        await new Promise((resolve) => setTimeout(resolve, 30000));

        // Check if still confirmed (not manually changed)
        const currentOrder = await admin.firestore()
          .collection("orders")
          .doc(orderId)
          .get();

        if (currentOrder.exists && currentOrder.data()?.status === "confirmed") {
          await admin.firestore()
            .collection("orders")
            .doc(orderId)
            .update({
              status: "preparing",
              preparingAt: admin.firestore.FieldValue.serverTimestamp(),
            });
          logger.info(`Order ${orderId} moved to preparing`);
        }
      } catch (error) {
        logger.error(`Error moving order ${orderId} to preparing:`, error);
      }
    }
  }
);

/**
 * Auto-deliver orders after 5 minutes of being marked "ready"
 */
export const autoDeliverReadyOrders = onDocumentUpdated(
  "orders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) return;

    // Check if order just became "ready"
    if (before.status !== "ready" && after.status === "ready") {
      logger.info(`Order ${orderId} marked as ready, will auto-deliver in 5 min`);

      try {
        // Wait 5 minutes
        await new Promise((resolve) => setTimeout(resolve, 300000));

        // Check if still ready (not manually changed)
        const currentOrder = await admin.firestore()
          .collection("orders")
          .doc(orderId)
          .get();

        if (currentOrder.exists && currentOrder.data()?.status === "ready") {
          await admin.firestore()
            .collection("orders")
            .doc(orderId)
            .update({
              status: "delivered",
              deliveredAt: admin.firestore.FieldValue.serverTimestamp(),
            });
          logger.info(`Order ${orderId} auto-delivered`);
        }
      } catch (error) {
        logger.error(`Error auto-delivering order ${orderId}:`, error);
      }
    }
  }
);

/**
 * Auto-progress group orders through status workflow:
 * pending -> confirmed (immediately) -> preparing (30s)
 */
export const autoProgressGroupOrders = onDocumentCreated(
  "groupOrders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const order = event.data?.data();

    if (!order) {
      logger.warn(`Group order ${orderId} has no data`);
      return;
    }

    logger.info(`New group order created: ${orderId}, status: ${order.status}`);

    try {
      // Immediately move to confirmed
      await admin.firestore()
        .collection("groupOrders")
        .doc(orderId)
        .update({
          status: "confirmed",
          confirmedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      logger.info(`Group order ${orderId} confirmed immediately`);
    } catch (error) {
      logger.error(`Error confirming group order ${orderId}:`, error);
    }
  }
);

/**
 * When group order is confirmed, schedule it to move to preparing after 30 seconds
 */
export const onGroupOrderConfirmed = onDocumentUpdated(
  "groupOrders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) return;

    // Check if order just became confirmed
    if (before.status !== "confirmed" && after.status === "confirmed") {
      logger.info(`Group order ${orderId} confirmed, will prepare in 30s`);

      try {
        // Wait 30 seconds
        await new Promise((resolve) => setTimeout(resolve, 30000));

        // Check if still confirmed (not manually changed)
        const currentOrder = await admin.firestore()
          .collection("groupOrders")
          .doc(orderId)
          .get();

        if (currentOrder.exists && currentOrder.data()?.status === "confirmed") {
          await admin.firestore()
            .collection("groupOrders")
            .doc(orderId)
            .update({
              status: "preparing",
              preparingAt: admin.firestore.FieldValue.serverTimestamp(),
            });
          logger.info(`Group order ${orderId} moved to preparing`);
        }
      } catch (error) {
        logger.error(`Error moving group order ${orderId} to preparing:`, error);
      }
    }
  }
);

/**
 * Auto-deliver group orders after 5 minutes of being marked "ready"
 */
export const autoDeliverReadyGroupOrders = onDocumentUpdated(
  "groupOrders/{orderId}",
  async (event) => {
    const orderId = event.params.orderId;
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) return;

    // Check if order just became "ready"
    if (before.status !== "ready" && after.status === "ready") {
      logger.info(`Group order ${orderId} marked as ready, will auto-deliver in 5 min`);

      try {
        // Wait 5 minutes
        await new Promise((resolve) => setTimeout(resolve, 300000));

        // Check if still ready (not manually changed)
        const currentOrder = await admin.firestore()
          .collection("groupOrders")
          .doc(orderId)
          .get();

        if (currentOrder.exists && currentOrder.data()?.status === "ready") {
          await admin.firestore()
            .collection("groupOrders")
            .doc(orderId)
            .update({
              status: "delivered",
              deliveredAt: admin.firestore.FieldValue.serverTimestamp(),
            });
          logger.info(`Group order ${orderId} auto-delivered`);
        }
      } catch (error) {
        logger.error(`Error auto-delivering group order ${orderId}:`, error);
      }
    }
  }
);

/**
 * Manual function to fix stuck pending orders
 * Call this from Firebase Console or via HTTP to fix existing orders
 */
export const fixStuckOrders = onRequest(async (request, response) => {
  try {
    logger.info("Starting fix for stuck orders...");

    // Fix individual orders stuck on pending — but ONLY paid/online orders
    // Cash orders with paymentStatus=unpaid must wait for admin to confirm payment
    const individualOrders = await admin.firestore()
      .collection("orders")
      .where("status", "==", "pending")
      .get();

    const individualPromises = individualOrders.docs
      .filter((doc) => doc.data().paymentStatus !== "unpaid" && doc.data().paymentMethod !== "cash")
      .map(async (doc) => {
        await doc.ref.update({
          status: "confirmed",
          confirmedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        logger.info(`Fixed individual order ${doc.id}`);
      });

    // Fix group orders stuck on pending
    const groupOrders = await admin.firestore()
      .collection("groupOrders")
      .where("status", "==", "pending")
      .get();

    const groupPromises = groupOrders.docs.map(async (doc) => {
      await doc.ref.update({
        status: "confirmed",
        confirmedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      logger.info(`Fixed group order ${doc.id}`);
    });

    await Promise.all([...individualPromises, ...groupPromises]);

    const totalFixed = individualOrders.size + groupOrders.size;
    logger.info(`Fixed ${totalFixed} stuck orders`);

    response.json({
      success: true,
      message: `Fixed ${totalFixed} stuck orders`,
      individual: individualOrders.size,
      group: groupOrders.size,
    });
  } catch (error) {
    logger.error("Error fixing stuck orders:", error);
    response.status(500).json({
      success: false,
      error: String(error),
    });
  }
});

/**
 * Join lobby redirect function
 * Creates clickable https:// links that redirect to the app
 * Usage: https://YOUR-PROJECT.cloudfunctions.net/joinLobby?code=ABC123
 */
export const joinLobby = onRequest((request, response) => {
  const code = request.query.code as string;

  if (!code) {
    response.status(400).send("Missing lobby code");
    return;
  }

  // Create HTML page that attempts app deep link and shows fallback
  const html = `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <title>Join WingZone Lobby</title>
      <style>
        body {
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          min-height: 100vh;
          margin: 0;
          padding: 20px;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          color: white;
          text-align: center;
        }
        .container {
          background: rgba(255, 255, 255, 0.95);
          color: #333;
          padding: 40px;
          border-radius: 20px;
          box-shadow: 0 20px 60px rgba(0,0,0,0.3);
          max-width: 400px;
        }
        h1 {
          margin: 0 0 20px 0;
          font-size: 28px;
        }
        .code {
          font-size: 36px;
          font-weight: bold;
          letter-spacing: 4px;
          color: #FF6B35;
          margin: 20px 0;
          padding: 15px;
          background: #fff;
          border-radius: 10px;
          border: 2px solid #FF6B35;
        }
        .instruction {
          margin: 20px 0;
          line-height: 1.6;
        }
        .opening {
          color: #4CAF50;
          font-weight: bold;
          margin: 10px 0;
        }
        .manual {
          margin-top: 30px;
          padding-top: 20px;
          border-top: 1px solid #ddd;
          font-size: 14px;
          color: #666;
        }
      </style>
    </head>
    <body>
      <div class="container">
        <h1>🍗 Join WingZone Lobby</h1>
        <div class="code">${code}</div>
        <p class="opening" id="status">Opening WingZone app...</p>
        <div class="manual">
          <p><strong>App didn't open?</strong></p>
          <p>1. Open the WingZone app manually<br>
          2. Go to <strong>Group Orders → Join Lobby</strong><br>
          3. Enter code: <strong>${code}</strong></p>
        </div>
      </div>
      <script>
        // Attempt to open the app with deep link
        setTimeout(() => {
          window.location.href = 'wz://join?code=${code}';
        }, 100);
        
        // Update status after attempt
        setTimeout(() => {
          document.getElementById('status').innerHTML = 
            'If the app opened, enter this code to join!';
        }, 2000);
      </script>
    </body>
    </html>
  `;

  response.set("Content-Type", "text/html");
  response.send(html);
});

/**
 * Create ToyyibPay bill for order payment
 * POST /createToyyibPayBill
 * Body: { orderId, customerName, customerEmail, totalAmount }
 * Returns: { billCode, paymentUrl }
 */
export const createToyyibPayBill = onRequest(
  {cors: true},
  async (request, response) => {
    // Only allow POST requests
    if (request.method !== "POST") {
      response.status(405).send("Method Not Allowed");
      return;
    }

    try {
      const {orderId, customerName, customerEmail, totalAmount} = request.body;

      // Validate required fields
      if (!orderId || !customerName || !customerEmail || !totalAmount) {
        response.status(400).json({
          error: "Missing required fields: orderId, customerName, customerEmail, totalAmount",
        });
        return;
      }

      // Get ToyyibPay credentials from Firebase config
      // Access using: firebase functions:config:set toyyibpay.secret_key="..." toyyibpay.category_code="..."
      const userSecretKey = "9bullxmy-9v7d-fqfc-hupo-zvzer7wo3i4x"; // From firebase config: toyyibpay.secret_key
      const categoryCode = "r90repsm"; // From firebase config: toyyibpay.category_code

      if (!userSecretKey || !categoryCode) {
        logger.error("ToyyibPay credentials not configured");
        response.status(500).json({
          error: "Payment gateway not configured",
        });
        return;
      }

      // Derive phone: strip all non-digit characters (guards against emails being passed).
      // ToyyibPay requires billPhone to be numeric and at least 7 digits.
      const rawPhone = (request.body.customerPhone ?? "").trim();
      const sanitizedPhone = rawPhone.replace(/\D/g, "");
      const customerPhone = sanitizedPhone.length >= 7 ? sanitizedPhone : "0100000000";
      logger.info("Resolved billPhone:", customerPhone, "(raw input:", rawPhone, ")");

      // Prepare ToyyibPay API request
      const toyyibPayParams = new URLSearchParams({
        userSecretKey: userSecretKey,
        categoryCode: categoryCode,
        billName: "Wing Zone Order",
        billDescription: `Payment for Order #${orderId}`,
        billPriceSetting: "1", // Fixed price
        billPayorInfo: "1", // Require payer info
        billAmount: Math.round(totalAmount * 100).toString(), // Convert to cents (integer)
        billReturnUrl: "wz://payment/success?order_id=" + orderId,
        billCallbackUrl: "https://us-central1-wingzone-app.cloudfunctions.net/paymentCallback",
        billExternalReferenceNo: orderId,
        billTo: customerName,
        billEmail: customerEmail,
        billPhone: customerPhone,
        billSplitPayment: "0",
        billSplitPaymentArgs: "",
        billPaymentChannel: "0", // All channels
        billContentEmail: `Thank you for your order at Wing Zone! Order #${orderId}`,
        billChargeToCustomer: "1", // Customer pays the processing fee
      });

      // Log the exact payload being sent to ToyyibPay (mask secret key)
      const debugParams = new URLSearchParams(toyyibPayParams);
      debugParams.set("userSecretKey", "[REDACTED]");
      logger.info("ToyyibPay request payload:", debugParams.toString());
      logger.info("ToyyibPay endpoint: https://dev.toyyibpay.com/index.php/api/createBill");
      logger.info("Resolved params — billAmount:", toyyibPayParams.get("billAmount"),
        "billPhone:", toyyibPayParams.get("billPhone"),
        "billEmail:", toyyibPayParams.get("billEmail"),
        "billTo:", toyyibPayParams.get("billTo"),
        "categoryCode:", toyyibPayParams.get("categoryCode"));

      // Make request to ToyyibPay API
      const toyyibPayResponse = await fetch(
        "https://dev.toyyibpay.com/index.php/api/createBill",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
          body: toyyibPayParams.toString(),
        }
      );

      // Always read raw text first so we can log it even if JSON parsing fails
      const rawResponseText = await toyyibPayResponse.text();
      logger.info("ToyyibPay raw HTTP status:", toyyibPayResponse.status);
      logger.info("ToyyibPay raw response body:", rawResponseText);

      // Safely parse JSON — ToyyibPay can return an HTML error page on misconfiguration
      let responseData: unknown;
      try {
        responseData = JSON.parse(rawResponseText);
      } catch (parseError) {
        logger.error("ToyyibPay response is not valid JSON. Raw body:", rawResponseText);
        response.status(500).json({
          error: "Payment gateway returned a non-JSON response",
          rawBody: rawResponseText.slice(0, 500), // truncate for safety
          httpStatus: toyyibPayResponse.status,
        });
        return;
      }

      // Check if bill creation was successful
      const rd = responseData as Record<string, unknown>;
      if (!toyyibPayResponse.ok || rd.error) {
        logger.error("ToyyibPay API error:", responseData);
        response.status(500).json({
          error: "Failed to create payment bill",
          details: responseData,
          rawBody: rawResponseText.slice(0, 500),
        });
        return;
      }

      // ToyyibPay returns an array with one element containing BillCode
      const dataArray = responseData as Array<Record<string, unknown>>;
      const billCode = dataArray[0]?.BillCode;

      if (!billCode) {
        logger.error("No BillCode in response:", responseData);
        response.status(500).json({
          error: "Invalid response from payment gateway",
        });
        return;
      }

      // Construct payment URL
      const paymentUrl = `https://dev.toyyibpay.com/${billCode}`;

      logger.info(`Created ToyyibPay bill for order ${orderId}: ${billCode}`);

      // Return bill code and payment URL to the app
      response.status(200).json({
        success: true,
        billCode: billCode,
        paymentUrl: paymentUrl,
        orderId: orderId,
      });
    } catch (error) {
      logger.error("Error creating ToyyibPay bill:", error);
      response.status(500).json({
        error: "Internal server error",
        message: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }
);

/**
 * Webhook to receive payment callbacks from ToyyibPay
 * POST /paymentCallback
 */
export const paymentCallback = onRequest(
  {cors: true},
  async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).send("Method Not Allowed");
      return;
    }

    try {
      const callbackData = request.body;
      logger.info("Payment callback received:", callbackData);

      // ToyyibPay sends: refno, status, reason, billcode, order_id, amount, etc.
      const {
        refno,
        status,
        reason,
        billcode,
        order_id: orderId,
      } = callbackData;

      if (!orderId) {
        logger.warn("Payment callback missing order_id");
        response.status(400).send("Missing order_id");
        return;
      }

      // Status codes from ToyyibPay:
      // 1 = Successful payment
      // 2 = Pending payment
      // 3 = Failed payment
      if (status === "1" || status === 1) {
        // Payment successful - update order in Firestore
        const orderRef = admin.firestore().collection("orders").doc(orderId);
        const orderDoc = await orderRef.get();

        if (orderDoc.exists) {
          await orderRef.update({
            paymentStatus: "paid",
            status: "confirmed",
            toyyibPayRefNo: refno,
            toyyibPayBillCode: billcode,
            paidAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          logger.info(`Order ${orderId} marked as paid (ref: ${refno})`);
        } else {
          logger.warn(`Order ${orderId} not found in database`);
        }
      } else {
        // Payment failed or pending
        logger.warn(
          `Payment status ${status} for order ${orderId}: ${reason}`
        );
      }

      // Always return 200 to acknowledge receipt
      response.status(200).send("OK");
    } catch (error) {
      logger.error("Error processing payment callback:", error);
      response.status(200).send("OK"); // Still acknowledge to avoid retries
    }
  }
);

