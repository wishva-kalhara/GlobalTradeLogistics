<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Shipments &mdash; Seller Portal</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
    <h1 class="text-xl font-semibold text-gray-900">My Shipments</h1>
    <p class="mt-1 text-sm text-gray-500">Shipments you've created, most recent first.</p>

    <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
    <div id="empty-state" class="mt-16 hidden flex-col items-center text-center">
        <p class="text-sm font-medium text-gray-900">No shipments yet</p>
        <p class="mt-1 text-sm text-gray-500">Create one from an open purchase order to see it here.</p>
    </div>

    <div id="shipment-list" class="mt-6 space-y-4"></div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.seller.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/seller/auth/login.jsp";
    }

    const STATUS_STYLES = {
        CREATED: "bg-gray-100 text-gray-700",
        IN_TRANSIT: "bg-blue-50 text-blue-700",
        DELIVERED: "bg-amber-50 text-amber-700",
        DELAYED: "bg-red-50 text-red-700",
        COMPLETED: "bg-green-50 text-green-700",
    };

    (async function loadShipments() {
        const errorEl = document.getElementById("alert-error");
        try {
            const res = await fetch("/api/v1/shipments/mine", {
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
            const shipments = await res.json();
            if (shipments.length === 0) {
                document.getElementById("empty-state").classList.remove("hidden");
                document.getElementById("empty-state").classList.add("flex");
                return;
            }

            const list = document.getElementById("shipment-list");
            shipments.forEach(function (sh) {
                const statusClass = STATUS_STYLES[sh.status] || "bg-gray-100 text-gray-700";
                const card = document.createElement("div");
                card.className = "rounded-2xl border border-gray-200 bg-white p-5 shadow-sm";
                card.innerHTML =
                    "<div class=\"flex items-center justify-between\">" +
                    "<h2 class=\"font-medium text-gray-900\">Shipment #" + sh.shipmentId + " &mdash; PO #" + sh.poId + "</h2>" +
                    "<span class=\"rounded-full px-3 py-1 text-xs font-medium " + statusClass + "\">" + sh.status + "</span>" +
                    "</div>" +
                    "<p class=\"mt-2 text-sm text-gray-700\">Tracking: " + sh.trackingNumber + " &middot; Vessel: " + sh.vesselId + " &middot; " + sh.type + "</p>" +
                    "<p class=\"mt-1 text-sm text-gray-500\">Customs status: " + (sh.customsStatus || "not yet filed") + "</p>";
                list.appendChild(card);
            });
        } catch (err) {
            errorEl.textContent = "Could not load shipments: " + err.message;
            errorEl.classList.remove("hidden");
        }
    })();
</script>
</body>
</html>
