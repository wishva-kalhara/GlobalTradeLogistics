<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Shipments &mdash; Staff Console</title>
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

        <div class="mt-6">
            <label class="mb-1 block text-sm font-medium text-gray-700">Shipment</label>
            <select id="shipmentId"
                    class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                <option value="">Select a shipment&hellip;</option>
            </select>
        </div>
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
                <div><dt class="text-gray-500">Purchase order</dt><dd id="sh-poId" class="font-medium text-gray-900"></dd></div>
                <div><dt class="text-gray-500">Customs status</dt><dd id="sh-customs-status" class="font-medium text-gray-900"></dd></div>
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
            <p class="mt-1 text-xs text-gray-400">Only available while the shipment is IN_TRANSIT. A GRN can't be recorded for this shipment until its customs status reaches CLEARED.</p>
            <form id="customs-form" class="mt-3 flex items-end gap-3">
                <div class="flex-1">
                    <label class="mb-1 block text-sm font-medium text-gray-700">Declaration number</label>
                    <input type="text" id="declaration-number" disabled
                           class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30 disabled:bg-gray-50 disabled:text-gray-400"/>
                </div>
                <button type="submit" disabled class="rounded-md bg-green-600 px-4 py-2 font-medium text-white hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50">Create Record</button>
            </form>

            <form id="customs-status-form" class="mt-4 flex items-end gap-3 border-t border-gray-100 pt-4">
                <div class="flex-1">
                    <label class="mb-1 block text-sm font-medium text-gray-700">Update customs status</label>
                    <select id="new-customs-status" disabled
                            class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30 disabled:bg-gray-50 disabled:text-gray-400">
                        <option value="PENDING">PENDING</option>
                        <option value="CLEARED">CLEARED</option>
                        <option value="HELD">HELD</option>
                    </select>
                </div>
                <button type="submit" disabled class="rounded-md bg-gray-900 px-4 py-2 font-medium text-white hover:bg-gray-800 disabled:cursor-not-allowed disabled:opacity-50">Update</button>
            </form>
        </div>

        <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            <h3 class="font-medium text-gray-900">Carrier system</h3>
            <p class="mt-1 text-sm text-gray-500">Simulates notifying the external carrier system and stores the reference it returns. Only available once customs status is CLEARED.</p>
            <button id="notify-carrier-btn" type="button" disabled class="mt-3 rounded-md border border-gray-300 px-4 py-2 font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50">Notify Carrier</button>
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
        document.getElementById("sh-poId").textContent = sh.poId ? ("#" + sh.poId) : "-";
        document.getElementById("sh-customs-status").textContent = sh.customsStatus || "-";
        document.getElementById("sh-ref").textContent = sh.ref || "-";

        const statusSelect = document.getElementById("new-status");
        statusSelect.innerHTML = "";
        (sh.status === "CREATED" ? ["IN_TRANSIT"] : ["IN_TRANSIT", "DELIVERED", "DELAYED"]).forEach(function (s) {
            statusSelect.appendChild(new Option(s, s));
        });
        statusSelect.value = sh.status;

        document.getElementById("new-customs-status").value = sh.customsStatus || "PENDING";

        const customsEnabled = sh.status === "IN_TRANSIT";
        document.getElementById("declaration-number").disabled = !customsEnabled;
        document.getElementById("new-customs-status").disabled = !customsEnabled;
        document.querySelector("#customs-form button[type='submit']").disabled = !customsEnabled;
        document.querySelector("#customs-status-form button[type='submit']").disabled = !customsEnabled;

        document.getElementById("notify-carrier-btn").disabled = sh.customsStatus !== "CLEARED";

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

    async function loadShipmentOptions(selectedShipmentId) {
        const select = document.getElementById("shipmentId");
        try {
            const res = await fetch("/api/v1/shipments", {
                headers: { "Authorization": "Bearer " + session.token },
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.app.session");
                window.location.href = "/app/login.jsp";
                return;
            }
            if (!res.ok) {
                const data = await res.json().catch(function () { return {}; });
                throw new Error(data.error || ("status " + res.status));
            }
            const shipments = await res.json();

            select.innerHTML = "";
            select.appendChild(new Option("Select a shipment…", ""));
            shipments.forEach(function (sh) {
                select.appendChild(new Option(
                        "Shipment #" + sh.shipmentId + " — " + sh.trackingNumber + " (" + sh.status + ")",
                        sh.shipmentId));
            });
            if (selectedShipmentId) {
                select.value = selectedShipmentId;
            }
        } catch (err) {
            showError("Could not load shipments: " + err.message);
        }
    }

    document.getElementById("shipmentId").addEventListener("change", async function () {
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");
        shipmentCard.classList.add("hidden");

        if (!this.value) {
            return;
        }

        try {
            const sh = await loadShipment(this.value);
            if (sh) {
                renderShipment(sh);
            }
        } catch (err) {
            showError("Could not load shipment: " + err.message);
        }
    });

    loadShipmentOptions();

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
            await loadShipmentOptions(currentShipmentId);
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
            renderShipment(await loadShipment(currentShipmentId));
        } catch (err) {
            showError("Could not create customs record: " + err.message);
        }
    });

    document.getElementById("customs-status-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        try {
            const res = await fetch("/api/v1/shipments/" + currentShipmentId + "/customs/status", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify({ status: document.getElementById("new-customs-status").value }),
            });
            const data = await res.json().catch(function () { return {}; });
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }
            renderShipment(data);
            showInfo("Shipment #" + data.shipmentId + " customs status updated to " + data.customsStatus + ".");
        } catch (err) {
            showError("Could not update customs status: " + err.message);
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
            showInfo("Carrier notified - reference " + data.ref + ".");
        } catch (err) {
            showError("Could not notify carrier: " + err.message);
        }
    });
</script>
</body>
</html>
