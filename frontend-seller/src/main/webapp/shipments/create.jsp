<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create Shipment &mdash; Seller Portal</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 class="text-xl font-semibold text-gray-900">Create Shipment</h1>
        <p class="mt-1 text-sm text-gray-500">Ship one of your open purchase orders &mdash; GlobalTrade's customs team picks it up from here.</p>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
        <div id="alert-info" class="mt-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>
        <div id="empty-state" class="mt-4 hidden text-sm text-gray-500">No purchase orders are ready to ship right now.</div>

        <form id="shipment-form" class="mt-6 space-y-4">
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Purchase order</label>
                <select id="poId" required
                        class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                    <option value="">Select a purchase order&hellip;</option>
                </select>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Tracking number</label>
                <input type="text" id="trackingNumber" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Vessel ID</label>
                <input type="text" id="vesselId" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Type</label>
                <select id="type" required
                        class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                    <option value="SEA">Sea</option>
                    <option value="AIR">Air</option>
                    <option value="ROAD">Road</option>
                    <option value="RAIL">Rail</option>
                </select>
            </div>
            <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Create Shipment</button>
        </form>
    </div>

    <div id="result-card" class="mt-6 hidden rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 class="font-medium text-gray-900">Shipment created</h2>
        <dl class="mt-3 grid grid-cols-2 gap-3 text-sm">
            <div><dt class="text-gray-500">Shipment ID</dt><dd id="result-shipmentId" class="font-medium text-gray-900"></dd></div>
            <div><dt class="text-gray-500">Purchase order</dt><dd id="result-poId" class="font-medium text-gray-900"></dd></div>
            <div><dt class="text-gray-500">Status</dt><dd id="result-status" class="font-medium text-gray-900"></dd></div>
        </dl>
        <p class="mt-3 text-xs text-gray-400">Hand this shipment ID to GlobalTrade's customs team &mdash; once they mark it DELIVERED, the warehouse manager can record the GRN.</p>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.seller.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/seller/auth/login.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const infoEl = document.getElementById("alert-info");
    const emptyEl = document.getElementById("empty-state");

    (async function loadShippablePurchaseOrders() {
        try {
            const res = await fetch("/api/v1/purchase-orders/shippable", {
                headers: { "Authorization": "Bearer " + session.token },
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.seller.session");
                window.location.href = "/seller/auth/login.jsp";
                return;
            }
            if (!res.ok) {
                const data = await res.json().catch(function () { return {}; });
                throw new Error(data.error || ("status " + res.status));
            }
            const orders = await res.json();
            if (orders.length === 0) {
                emptyEl.classList.remove("hidden");
                document.getElementById("shipment-form").classList.add("hidden");
                return;
            }

            const select = document.getElementById("poId");
            orders.forEach(function (po) {
                const option = document.createElement("option");
                option.value = po.poId;
                option.textContent = "PO #" + po.poId + " — " + po.requestingQty + " × " + po.productName;
                select.appendChild(option);
            });
        } catch (err) {
            errorEl.textContent = "Could not load purchase orders: " + err.message;
            errorEl.classList.remove("hidden");
        }
    })();

    document.getElementById("shipment-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");
        document.getElementById("result-card").classList.add("hidden");

        const poId = document.getElementById("poId").value;
        const body = {
            trackingNumber: document.getElementById("trackingNumber").value,
            vesselId: document.getElementById("vesselId").value,
            type: document.getElementById("type").value,
        };

        try {
            const res = await fetch("/api/v1/purchase-orders/" + poId + "/shipment", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify(body),
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.seller.session");
                window.location.href = "/seller/auth/login.jsp";
                return;
            }
            const data = await res.json().catch(function () { return {}; });
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }

            infoEl.textContent = "Shipment #" + data.shipmentId + " created.";
            infoEl.classList.remove("hidden");

            document.getElementById("result-shipmentId").textContent = "#" + data.shipmentId;
            document.getElementById("result-poId").textContent = "#" + data.poId;
            document.getElementById("result-status").textContent = data.status;
            document.getElementById("result-card").classList.remove("hidden");

            document.getElementById("shipment-form").reset();
            document.getElementById("poId").querySelector("option[value='" + poId + "']").remove();
        } catch (err) {
            errorEl.textContent = "Could not create shipment: " + err.message;
            errorEl.classList.remove("hidden");
        }
    });
</script>
</body>
</html>
