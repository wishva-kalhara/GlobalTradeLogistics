<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Warehouse Inventory — Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
            <h1 class="text-xl font-semibold text-gray-900">Warehouse Inventory</h1>
            <p class="mt-1 text-sm text-gray-500">Current stock levels — rows below reorder level are highlighted.</p>
        </div>
        <div>
            <label class="mb-1 block text-xs font-medium text-gray-700">Warehouse</label>
            <select id="warehouseId"
                    class="block w-56 rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"></select>
        </div>
    </div>

    <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
    <div id="empty-state" class="mt-16 hidden text-center text-sm text-gray-500">No inventory rows found for that warehouse.</div>

    <div class="mt-6 overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200 text-sm">
            <thead>
            <tr class="text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                <th class="py-2 pr-4">Product</th>
                <th class="py-2 pr-4">Qty</th>
                <th class="py-2 pr-4">Reorder level</th>
                <th class="py-2 pr-4">Unit price</th>
                <th class="py-2 pr-4">Last updated</th>
            </tr>
            </thead>
            <tbody id="inventory-table-body" class="divide-y divide-gray-100"></tbody>
        </table>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/app/login.jsp";
    } else if (!["ADMIN", "COORDINATOR", "WAREHOUSE_MANAGER"].includes(session.role)) {
        window.location.href = "/app/access-denied.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const emptyEl = document.getElementById("empty-state");
    const body = document.getElementById("inventory-table-body");

    async function loadInventory(warehouseId) {
        errorEl.classList.add("hidden");
        emptyEl.classList.add("hidden");
        body.innerHTML = "";

        try {
            const res = await fetch("/api/v1/inventory/" + warehouseId, {
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
            const rows = await res.json();
            if (rows.length === 0) {
                emptyEl.classList.remove("hidden");
                return;
            }

            rows.forEach(function (r) {
                const low = r.qty < r.reorderLevel;
                const row = document.createElement("tr");
                if (low) {
                    row.className = "bg-amber-50";
                }
                row.innerHTML =
                    "<td class=\"py-2 pr-4 text-gray-900\">" + r.productName + "</td>" +
                    "<td class=\"py-2 pr-4 " + (low ? "font-medium text-amber-700" : "text-gray-700") + "\">" + r.qty + (low ? " (low)" : "") + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-500\">" + r.reorderLevel + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-700\">$" + r.unitPrice.toFixed(2) + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-500\">" + new Date(r.lastUpdatedAt).toLocaleString() + "</td>";
                body.appendChild(row);
            });
        } catch (err) {
            errorEl.textContent = "Could not load inventory: " + err.message;
            errorEl.classList.remove("hidden");
        }
    }

    async function loadWarehouses() {
        const select = document.getElementById("warehouseId");
        try {
            const res = await fetch("/api/v1/inventory/warehouses", {
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
            const warehouses = await res.json();
            warehouses.forEach(function (w) {
                const option = document.createElement("option");
                option.value = w.warehouseId;
                option.textContent = "Warehouse " + w.warehouseId + " (" + w.country + ")";
                select.appendChild(option);
            });
            if (warehouses.length > 0) {
                loadInventory(select.value);
            } else {
                emptyEl.classList.remove("hidden");
            }
        } catch (err) {
            errorEl.textContent = "Could not load warehouses: " + err.message;
            errorEl.classList.remove("hidden");
        }
    }

    document.getElementById("warehouseId").addEventListener("change", function () {
        loadInventory(this.value);
    });

    loadWarehouses();
</script>
</body>
</html>
