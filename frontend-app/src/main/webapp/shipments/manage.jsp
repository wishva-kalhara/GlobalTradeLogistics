<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Shipments — Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 class="text-xl font-semibold text-gray-900">Manage Shipments</h1>
        <p class="mt-1 text-sm text-gray-500">Look up a shipment to update its status, record customs clearance, or notify the carrier system.</p>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
        <div id="alert-info" class="mt-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

        <form id="lookup-form" class="mt-6 flex items-end gap-3">
            <div class="flex-1">
                <label class="mb-1 block text-sm font-medium text-gray-700">Shipment ID</label>
                <input type="number" id="shipmentId" min="1" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <button type="submit" class="rounded-md bg-gray-900 px-4 py-2 font-medium text-white hover:bg-gray-800">Load</button>
        </form>
    </div>

    <div id="shipment-card" class="mt-6 hidden space-y-6">
        <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            <div class="flex items-center justify-between">
                <h2 class="font-medium text-gray-900">Shipment #<span id="sh-id"></span></h2>
                <span id="sh-status" class="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700"></span>
            </div>
            <dl class="mt-3 grid grid-cols-2 gap-3 text-sm">
                <div><dt class="text-gray-500">Tracking number</dt><dd id="sh-tracking" class="font-medium text-gray-900"></dd></div>
                <div><dt class="text-gray-500">Vessel</dt><dd id="sh-vessel" class="font-medium text-gray-900"></dd></div>
                <div><dt class="text-gray-500">Type</dt><dd id="sh-type" class="font-medium text-gray-900"></dd></div>
                <div><dt class="text-gray-500">Warehouse</dt><dd id="sh-warehouse" class="font-medium text-gray-900"></dd></div>
                <div><dt class="text-gray-500">Carrier ref</dt><dd id="sh-ref" class="font-medium text-gray-900">&mdash;</dd></div>
            </dl>
        </div>

        <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 class="font-medium text-gray-900">Update status</h3>
            <form id="status-form" class="mt-3 flex items-end gap-3">
                <div class="flex-1">
                    <label class="mb-1 block text-sm font-medium text-gray-700">New status</label>
                    <select id="new-status"
                            class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                        <option value="CREATED">CREATED</option>
                        <option value="IN_TRANSIT">IN_TRANSIT</option>
                        <option value="DELIVERED">DELIVERED</option>
                        <option value="DELAYED">DELAYED</option>
                    </select>
                </div>
                <button type="submit" class="rounded-md bg-green-600 px-4 py-2 font-medium text-white hover:bg-green-700">Update</button>
            </form>
        </div>

        <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 class="font-medium text-gray-900">Record customs clearance</h3>
            <form id="customs-form" class="mt-3 flex items-end gap-3">
                <div class="flex-1">
                    <label class="mb-1 block text-sm font-medium text-gray-700">Declaration number</label>
                    <input type="text" id="declaration-number"
                           class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                </div>
                <button type="submit" class="rounded-md bg-green-600 px-4 py-2 font-medium text-white hover:bg-green-700">Create Record</button>
            </form>
        </div>

        <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 class="font-medium text-gray-900">Carrier system</h3>
            <p class="mt-1 text-sm text-gray-500">Simulates notifying the external carrier system and stores the reference it returns.</p>
            <button id="notify-carrier-btn" type="button" class="mt-3 rounded-md border border-gray-300 px-4 py-2 font-medium text-gray-700 hover:bg-gray-50">Notify Carrier</button>
        </div>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/app/login.jsp";
    } else if (session.role !== "CUSTOMS_AGENT") {
        window.location.href = "/app/access-denied.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const infoEl = document.getElementById("alert-info");
    const shipmentCard = document.getElementById("shipment-card");
    let currentShipmentId = null;

    function showError(message) {
        errorEl.textContent = message;
        errorEl.classList.remove("hidden");
        infoEl.classList.add("hidden");
    }

    function showInfo(message) {
        infoEl.textContent = message;
        infoEl.classList.remove("hidden");
        errorEl.classList.add("hidden");
    }

    function renderShipment(sh) {
        currentShipmentId = sh.shipmentId;
        document.getElementById("sh-id").textContent = sh.shipmentId;
        document.getElementById("sh-status").textContent = sh.status;
        document.getElementById("sh-tracking").textContent = sh.trackingNumber;
        document.getElementById("sh-vessel").textContent = sh.vesselId;
        document.getElementById("sh-type").textContent = sh.type;
        document.getElementById("sh-warehouse").textContent = sh.warehouseId;
        document.getElementById("sh-ref").textContent = sh.ref || "—";
        document.getElementById("new-status").value = sh.status;
        shipmentCard.classList.remove("hidden");
    }

    async function loadShipment(shipmentId) {
        const res = await fetch("/api/v1/shipments/" + shipmentId, {
            headers: { "Authorization": "Bearer " + session.token },
        });
        if (res.status === 401) {
            localStorage.removeItem("gtl.app.session");
            window.location.href = "/app/login.jsp";
            return null;
        }
        const data = await res.json().catch(function () { return {}; });
        if (!res.ok) {
            throw new Error(data.error || ("status " + res.status));
        }
        return data;
    }

    document.getElementById("lookup-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");
        shipmentCard.classList.add("hidden");

        try {
            const sh = await loadShipment(document.getElementById("shipmentId").value);
            if (sh) {
                renderShipment(sh);
            }
        } catch (err) {
            showError("Could not load shipment: " + err.message);
        }
    });

    document.getElementById("status-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        try {
            const res = await fetch("/api/v1/shipments/" + currentShipmentId + "/status", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify({
                    status: document.getElementById("new-status").value,
                    idempotencyKey: crypto.randomUUID(),
                }),
            });
            const data = await res.json().catch(function () { return {}; });
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }
            renderShipment(data);
            showInfo("Shipment #" + data.shipmentId + " status updated to " + data.status + ".");
        } catch (err) {
            showError("Could not update status: " + err.message);
        }
    });

    document.getElementById("customs-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        try {
            const res = await fetch("/api/v1/shipments/" + currentShipmentId + "/customs", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify({ declarationNumber: document.getElementById("declaration-number").value }),
            });
            if (!res.ok) {
                const data = await res.json().catch(function () { return {}; });
                throw new Error(data.error || ("status " + res.status));
            }
            showInfo("Customs clearance record created for shipment #" + currentShipmentId + ".");
            document.getElementById("customs-form").reset();
        } catch (err) {
            showError("Could not create customs record: " + err.message);
        }
    });

    document.getElementById("notify-carrier-btn").addEventListener("click", async function () {
        try {
            const res = await fetch("/api/v1/shipments/" + currentShipmentId + "/notify-carrier", {
                method: "POST",
                headers: { "Authorization": "Bearer " + session.token },
            });
            const data = await res.json().catch(function () { return {}; });
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }
            renderShipment(data);
            showInfo("Carrier notified — reference " + data.ref + ".");
        } catch (err) {
            showError("Could not notify carrier: " + err.message);
        }
    });
</script>
</body>
</html>
