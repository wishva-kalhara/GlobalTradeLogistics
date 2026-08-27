<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Record GRN &mdash; Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-3xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 class="text-xl font-semibold text-gray-900">Record Goods Received (GRN)</h1>
        <p class="mt-1 text-sm text-gray-500">Confirm goods that arrived against an open purchase order &mdash; this adds the quantity back into stock and marks the PO complete.</p>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
        <div id="alert-info" class="mt-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

        <form id="grn-form" class="mt-6 space-y-4">
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Purchase order ID</label>
                <input type="number" id="poId" min="1" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Quantity received</label>
                <input type="number" id="qty" min="1" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Record GRN</button>
        </form>
    </div>

    <div id="result-card" class="mt-6 hidden rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h2 class="font-medium text-gray-900">Purchase order updated</h2>
        <dl class="mt-3 grid grid-cols-2 gap-3 text-sm">
            <div><dt class="text-gray-500">PO ID</dt><dd id="result-poId" class="font-medium text-gray-900"></dd></div>
            <div><dt class="text-gray-500">Product</dt><dd id="result-product" class="font-medium text-gray-900"></dd></div>
            <div><dt class="text-gray-500">Status</dt><dd id="result-status" class="font-medium text-gray-900"></dd></div>
        </dl>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/app/login.jsp";
    } else if (session.role !== "WAREHOUSE_MANAGER") {
        window.location.href = "/app/access-denied.jsp";
    }

    const errorEl = document.getElementById("alert-error");
    const infoEl = document.getElementById("alert-info");

    document.getElementById("grn-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");
        document.getElementById("result-card").classList.add("hidden");

        const poId = document.getElementById("poId").value;
        const body = { qty: parseInt(document.getElementById("qty").value, 10) };

        try {
            const res = await fetch("/api/v1/purchase-orders/" + poId + "/grn", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify(body),
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.app.session");
                window.location.href = "/app/login.jsp";
                return;
            }
            const data = await res.json().catch(function () { return {}; });
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }

            infoEl.textContent = "GRN recorded for purchase order #" + data.poId + ".";
            infoEl.classList.remove("hidden");

            document.getElementById("result-poId").textContent = "#" + data.poId;
            document.getElementById("result-product").textContent = data.productName;
            document.getElementById("result-status").textContent = data.completed ? "Completed" : "Still open";
            document.getElementById("result-card").classList.remove("hidden");

            document.getElementById("grn-form").reset();
        } catch (err) {
            errorEl.textContent = "Could not record GRN: " + err.message;
            errorEl.classList.remove("hidden");
        }
    });
</script>
</body>
</html>
