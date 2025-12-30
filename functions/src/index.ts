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

    logger.info(`New order created: ${orderId}, status: ${order.status}`);

    try {
      // Immediately move to confirmed
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
import {onRequest} from "firebase-functions/v2/https";

export const fixStuckOrders = onRequest(async (request, response) => {
  try {
    logger.info("Starting fix for stuck orders...");

    // Fix individual orders stuck on pending
    const individualOrders = await admin.firestore()
      .collection("orders")
      .where("status", "==", "pending")
      .get();

    const individualPromises = individualOrders.docs.map(async (doc) => {
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
