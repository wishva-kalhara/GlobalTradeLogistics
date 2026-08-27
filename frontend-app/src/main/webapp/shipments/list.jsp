<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shipments &mdash; Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
    <h1 class="text-xl font-semibold text-gray-900">Shipments</h1>
    <p class="mt-1 text-sm text-gray-500">Every shipment currently in the ship &rarr; customs &rarr; GRN pipeline, most recent first.</p>

    <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
    <div id="empty-state" class="mt-16 hidden text-center text-sm text-gray-500">No shipments yet.</div>

    <div class="mt-6 overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200 text-sm">
            <thead>
            <tr class="text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <th class="py-2 pr-4">Shipment</th>
                <th class="py-2 pr-4">PO</th>
                <th class="py-2 pr-4">Tracking</th>
                <th class="py-2 pr-4">Status</th>
                <th class="py-2 pr-4">Customs</th>
            </tr>
            </thead>
            <tbody id="shipments-table-body" class="divide-y divide-gray-100"></tbody>
        </table>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/app/login.jsp";
    } else if (!["ADMIN", "COORDINATOR", "WAREHOUSE_MANAGER", "CUSTOMS_AGENT"].includes(session.role)) {
        window.location.href = "/app/access-denied.jsp";
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
            if (shipments.length === 0) {
                document.getElementById("empty-state").classList.remove("hidden");
                return;
            }

            const body = document.getElementById("shipments-table-body");
            shipments.forEach(function (sh) {
                const statusClass = STATUS_STYLES[sh.status] || "bg-gray-100 text-gray-700";
                const row = document.createElement("tr");
                row.innerHTML =
                    "<td class=\"py-2 pr-4 text-gray-900\">#" + sh.shipmentId + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-500\">" + (sh.poId ? ("#" + sh.poId) : "-") + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-700\">" + sh.trackingNumber + "</td>" +
                    "<td class=\"py-2 pr-4\"><span class=\"rounded-full px-2 py-1 text-xs font-medium " + statusClass + "\">" + sh.status + "</span></td>" +
                    "<td class=\"py-2 pr-4 text-gray-500\">" + (sh.customsStatus || "-") + "</td>";
                body.appendChild(row);
            });
        } catch (err) {
            errorEl.textContent = "Could not load shipments: " + err.message;
            errorEl.classList.remove("hidden");
        }
    })();
</script>
</body>
</html>
