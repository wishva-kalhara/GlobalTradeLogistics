<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Products — GlobalTrade Logistics</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-6xl px-4 py-10 pb-32 sm:px-6 lg:px-8">
    <div class="flex flex-wrap items-end justify-between gap-4">
        <div>
            <h1 class="text-xl font-semibold text-gray-900">Products</h1>
            <p class="mt-1 text-sm text-gray-500">Pick a quantity for anything you'd like to order, then place your order.</p>
        </div>
        <div class="relative w-full max-w-xs">
            <svg class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" viewBox="0 0 20 20" fill="currentColor">
                <path fill-rule="evenodd" d="M9 3.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11zM2 9a7 7 0 1112.452 4.391l3.328 3.329a.75.75 0 11-1.06 1.06l-3.329-3.328A7 7 0 012 9z" clip-rule="evenodd"/>
            </svg>
            <input id="search-input" type="search" placeholder="Search products&hellip;"
                   class="block w-full rounded-md border border-gray-300 py-2 pl-9 pr-3 text-sm shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
        </div>
    </div>

    <div id="alert-error" class="mt-6 hidden items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
        <svg class="mt-0.5 h-4 w-4 flex-none" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8.485 3.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 3.495zM10 6a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 6zm0 8a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/>
        </svg>
        <span id="alert-error-text"></span>
    </div>
    <div id="alert-info" class="mt-6 hidden items-start gap-2 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
        <svg class="mt-0.5 h-4 w-4 flex-none" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.857-9.809a.75.75 0 00-1.214-.882l-3.483 4.79-1.88-1.88a.75.75 0 10-1.06 1.061l2.5 2.5a.75.75 0 001.137-.089l4-5.5z" clip-rule="evenodd"/>
        </svg>
        <span id="alert-info-text"></span>
    </div>

    <div id="loading-skeleton" class="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3"></div>

    <div id="empty-state" class="mt-16 hidden flex-col items-center text-center">
        <svg class="h-12 w-12 text-gray-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M20.25 7.5l-.625 10.632a2.25 2.25 0 01-2.247 2.118H6.622a2.25 2.25 0 01-2.247-2.118L3.75 7.5M10 11.25h4M3.375 7.5h17.25c.621 0 1.125-.504 1.125-1.125v-1.5c0-.621-.504-1.125-1.125-1.125H3.375C2.754 3.75 2.25 4.254 2.25 4.875v1.5c0 .621.504 1.125 1.125 1.125z"/>
        </svg>
        <p class="mt-3 text-sm font-medium text-gray-900">No products match your search</p>
        <p class="mt-1 text-sm text-gray-500">Try a different search term.</p>
    </div>

    <div id="product-grid" class="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3"></div>
</main>

<div id="cart-bar" class="fixed inset-x-0 bottom-0 z-20 hidden border-t border-gray-200 bg-white/95 backdrop-blur">
    <div class="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
        <div class="text-sm text-gray-700">
            <span id="cart-count" class="font-semibold text-gray-900">0 items</span>
            selected &middot; Total
            <span id="cart-total" class="font-semibold text-gray-900">$0.00</span>
        </div>
        <button id="place-order-btn" type="button"
                class="inline-flex items-center gap-2 rounded-md bg-green-600 px-5 py-2.5 font-medium text-white shadow-sm hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50">
            <svg id="place-order-spinner" class="hidden h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"/>
            </svg>
            <span id="place-order-label">Place Order</span>
        </button>
    </div>
</div>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.customer.session") || "null");
    const errorEl = document.getElementById("alert-error");
    const errorTextEl = document.getElementById("alert-error-text");
    const infoEl = document.getElementById("alert-info");
    const infoTextEl = document.getElementById("alert-info-text");
    const grid = document.getElementById("product-grid");
    const skeleton = document.getElementById("loading-skeleton");
    const emptyState = document.getElementById("empty-state");
    const searchInput = document.getElementById("search-input");
    const cartBar = document.getElementById("cart-bar");
    const cartCountEl = document.getElementById("cart-count");
    const cartTotalEl = document.getElementById("cart-total");
    const placeOrderBtn = document.getElementById("place-order-btn");
    const placeOrderSpinner = document.getElementById("place-order-spinner");
    const placeOrderLabel = document.getElementById("place-order-label");

    let products = [];
    const quantities = {};

    const SKELETON_CARD =
        "<div class=\"animate-pulse rounded-2xl border border-gray-200 bg-white p-5\">" +
        "<div class=\"h-24 rounded-xl bg-gray-100\"></div>" +
        "<div class=\"mt-4 h-4 w-2/3 rounded bg-gray-100\"></div>" +
        "<div class=\"mt-2 h-3 w-full rounded bg-gray-100\"></div>" +
        "<div class=\"mt-2 h-3 w-5/6 rounded bg-gray-100\"></div>" +
        "<div class=\"mt-4 h-9 w-full rounded bg-gray-100\"></div>" +
        "</div>";
    skeleton.innerHTML = SKELETON_CARD.repeat(6);

    function showError(message) {
        errorTextEl.textContent = message;
        errorEl.classList.remove("hidden");
        errorEl.classList.add("flex");
        infoEl.classList.add("hidden");
    }

    function showInfo(message) {
        infoTextEl.textContent = message;
        infoEl.classList.remove("hidden");
        infoEl.classList.add("flex");
        errorEl.classList.add("hidden");
    }

    function hideAlerts() {
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");
    }

    function stockBadge(p) {
        if (p.availableQty <= 0) {
            return "<span class=\"absolute right-3 top-3 rounded-full bg-red-100 px-2.5 py-1 text-xs font-medium text-red-700\">Out of stock</span>";
        }
        if (p.availableQty <= 10) {
            return "<span class=\"absolute right-3 top-3 rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-700\">Only " + p.availableQty + " left</span>";
        }
        return "<span class=\"absolute right-3 top-3 rounded-full bg-green-100 px-2.5 py-1 text-xs font-medium text-green-700\">In stock</span>";
    }

    function renderProducts(list) {
        grid.innerHTML = "";
        if (list.length === 0) {
            emptyState.classList.remove("hidden");
            emptyState.classList.add("flex");
            return;
        }
        emptyState.classList.add("hidden");
        emptyState.classList.remove("flex");

        list.forEach(function (p) {
            const disabled = p.availableQty <= 0;
            const qty = quantities[p.productId] || 0;

            const card = document.createElement("div");
            card.className = "flex flex-col rounded-2xl border border-gray-200 bg-white p-5 shadow-sm transition hover:shadow-md";
            card.innerHTML =
                "<div class=\"relative flex h-24 items-center justify-center overflow-hidden rounded-xl bg-gradient-to-br from-green-50 to-emerald-100\">" +
                "<svg class=\"h-10 w-10 text-green-600/70\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\">" +
                "<path stroke-linecap=\"round\" stroke-linejoin=\"round\" d=\"M20.25 7.5l-8.25 4.5-8.25-4.5M20.25 7.5l-8.25-4.5-8.25 4.5M20.25 7.5v9l-8.25 4.5m0-9v9m0-9L3.75 7.5m8.25 13.5L3.75 16.5v-9\"/>" +
                "</svg>" +
                stockBadge(p) +
                "</div>" +
                "<h2 class=\"mt-4 font-medium text-gray-900\">" + p.name + "</h2>" +
                "<p class=\"mt-1 flex-1 text-sm text-gray-500 line-clamp-2\">" + p.description + "</p>" +
                "<p class=\"mt-3 text-lg font-semibold text-gray-900\">$" + p.unitPrice.toFixed(2) + "</p>" +
                "<div class=\"mt-3 flex items-center gap-2\">" +
                "<button type=\"button\" data-action=\"dec\" data-product-id=\"" + p.productId + "\" " + (disabled ? "disabled" : "") +
                "class=\"qty-btn flex h-9 w-9 flex-none items-center justify-center rounded-md border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40\">&minus;</button>" +
                "<input type=\"number\" min=\"0\" max=\"" + p.availableQty + "\" value=\"" + qty + "\" data-product-id=\"" + p.productId + "\" " + (disabled ? "disabled" : "") +
                "class=\"qty-input w-full rounded-md border border-gray-300 px-3 py-2 text-center shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30 disabled:bg-gray-50 disabled:text-gray-400\"/>" +
                "<button type=\"button\" data-action=\"inc\" data-product-id=\"" + p.productId + "\" " + (disabled ? "disabled" : "") +
                "class=\"qty-btn flex h-9 w-9 flex-none items-center justify-center rounded-md border border-gray-300 text-gray-600 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40\">&plus;</button>" +
                "</div>";
            grid.appendChild(card);
        });

        document.querySelectorAll(".qty-input").forEach(function (input) {
            input.addEventListener("input", function () {
                setQuantity(input, parseInt(input.value, 10) || 0);
            });
        });
        document.querySelectorAll(".qty-btn").forEach(function (btn) {
            btn.addEventListener("click", function () {
                const productId = parseInt(btn.dataset.productId, 10);
                const input = grid.querySelector(".qty-input[data-product-id=\"" + productId + "\"]");
                const delta = btn.dataset.action === "inc" ? 1 : -1;
                setQuantity(input, (parseInt(input.value, 10) || 0) + delta);
            });
        });
    }

    function setQuantity(input, value) {
        const max = parseInt(input.max, 10);
        const clamped = Math.max(0, Math.min(isNaN(max) ? value : max, value));
        input.value = clamped;
        quantities[parseInt(input.dataset.productId, 10)] = clamped;
        updateCartBar();
    }

    function updateCartBar() {
        let count = 0;
        let total = 0;
        Object.keys(quantities).forEach(function (productId) {
            const qty = quantities[productId];
            if (qty > 0) {
                const product = products.find(function (p) { return p.productId === parseInt(productId, 10); });
                if (product) {
                    count += qty;
                    total += qty * product.unitPrice;
                }
            }
        });
        cartCountEl.textContent = count + (count === 1 ? " item" : " items");
        cartTotalEl.textContent = "$" + total.toFixed(2);
        cartBar.classList.toggle("hidden", count === 0);
        cartBar.classList.toggle("flex", count > 0);
    }

    (async function loadProducts() {
        try {
            const res = await fetch("/api/v1/products");
            if (!res.ok) {
                throw new Error("status " + res.status);
            }
            products = await res.json();
            skeleton.classList.add("hidden");
            renderProducts(products);
        } catch (err) {
            skeleton.classList.add("hidden");
            showError("Could not load products: " + err.message);
        }
    })();

    searchInput.addEventListener("input", function () {
        const term = searchInput.value.trim().toLowerCase();
        const filtered = term
            ? products.filter(function (p) { return p.name.toLowerCase().includes(term) || p.description.toLowerCase().includes(term); })
            : products;
        renderProducts(filtered);
    });

    placeOrderBtn.addEventListener("click", async function () {
        hideAlerts();

        if (!session || !session.token) {
            window.location.href = "/auth/login.jsp";
            return;
        }

        const items = [];
        Object.keys(quantities).forEach(function (productId) {
            if (quantities[productId] > 0) {
                items.push({productId: parseInt(productId, 10), qty: quantities[productId]});
            }
        });

        if (items.length === 0) {
            showError("Pick a quantity greater than zero for at least one product.");
            return;
        }

        placeOrderBtn.disabled = true;
        placeOrderSpinner.classList.remove("hidden");
        placeOrderLabel.textContent = "Placing order…";

        try {
            const res = await fetch("/api/v1/orders", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify({items: items}),
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.customer.session");
                window.location.href = "/auth/login.jsp";
                return;
            }
            const data = await res.json().catch(function () { return {}; });
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }
            showInfo("Order #" + data.orderId + " placed — total $" + data.totalPrice.toFixed(2) + ". View it under My Orders.");
            for (const key in quantities) {
                quantities[key] = 0;
            }
            document.querySelectorAll(".qty-input").forEach(function (input) { input.value = 0; });
            updateCartBar();
            window.scrollTo({top: 0, behavior: "smooth"});
        } catch (err) {
            showError("Could not place order: " + err.message);
        } finally {
            placeOrderBtn.disabled = false;
            placeOrderSpinner.classList.add("hidden");
            placeOrderLabel.textContent = "Place Order";
        }
    });
</script>
</body>
</html>
