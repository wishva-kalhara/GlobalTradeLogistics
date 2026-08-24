<nav class="border-b border-gray-200 bg-white">
    <div class="mx-auto flex h-16 max-w-5xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <a href="/app/index.jsp" class="flex items-center gap-2">
            <span class="inline-flex h-8 w-8 items-center justify-center rounded-lg bg-green-600 text-sm font-bold text-white">GT</span>
            <span class="text-lg font-semibold text-gray-900">GlobalTrade Logistics — Staff Console</span>
        </a>
        <div class="flex items-center gap-3 text-sm">
            <span id="nav-session" class="hidden text-gray-500 sm:inline"></span>
            <div id="nav-account" class="relative hidden">
                <button id="nav-account-btn" type="button"
                        class="inline-flex items-center gap-1 rounded-md bg-gray-100 px-3 py-1.5 font-medium text-gray-700 hover:bg-gray-200">
                    Account
                    <svg class="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                        <path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.293l3.71-4.06a.75.75 0 111.08 1.04l-4.25 4.65a.75.75 0 01-1.08 0l-4.25-4.65a.75.75 0 01.02-1.06z" clip-rule="evenodd"/>
                    </svg>
                </button>
                <!-- Populated by JS below, based on session.role -->
                <div id="nav-account-menu"
                     class="absolute right-0 z-10 mt-2 hidden w-56 rounded-md border border-gray-200 bg-white py-1 shadow-lg"></div>
            </div>
            <a id="nav-logout-link" href="#" class="hidden rounded-md bg-gray-100 px-3 py-1.5 font-medium text-gray-700 hover:bg-gray-200">Log out</a>
            <a id="nav-login-link" href="/app/login.jsp" class="hidden rounded-md bg-green-600 px-3 py-1.5 font-medium text-white hover:bg-green-700">Log in</a>
        </div>
    </div>
</nav>

<script>
    (function () {
        // One entry per role that currently has a real page. WORKER has none yet.
        const ROLE_FUNCTIONS = {
            ADMIN: [
                { href: "/app/app-user-management.jsp", label: "Application User Management" },
                { href: "/app/vendor-performance.jsp", label: "Vendor Performance Report" },
                { href: "/app/inventory.jsp", label: "Warehouse Inventory" },
            ],
            COORDINATOR: [
                { href: "/app/purchase-orders/create.jsp", label: "Create Purchase Order" },
                { href: "/app/vendor-performance.jsp", label: "Vendor Performance Report" },
                { href: "/app/inventory.jsp", label: "Warehouse Inventory" },
            ],
            WAREHOUSE_MANAGER: [
                { href: "/app/purchase-orders/record-grn.jsp", label: "Record GRN" },
                { href: "/app/inventory.jsp", label: "Warehouse Inventory" },
            ],
            CUSTOMS_AGENT: [
                { href: "/app/shipments/manage.jsp", label: "Manage Shipments" },
            ],
        };

        const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
        const sessionEl = document.getElementById("nav-session");
        const accountEl = document.getElementById("nav-account");
        const accountBtn = document.getElementById("nav-account-btn");
        const accountMenu = document.getElementById("nav-account-menu");
        const logoutLink = document.getElementById("nav-logout-link");
        const loginLink = document.getElementById("nav-login-link");

        if (session && session.token) {
            sessionEl.textContent = session.email + " · " + session.role;
            sessionEl.classList.remove("hidden");
            accountEl.classList.remove("hidden");
            logoutLink.classList.remove("hidden");

            const functions = ROLE_FUNCTIONS[session.role] || [];
            if (functions.length === 0) {
                const empty = document.createElement("div");
                empty.className = "px-4 py-2 text-sm text-gray-400";
                empty.textContent = "No actions available";
                accountMenu.appendChild(empty);
            } else {
                functions.forEach(function (fn) {
                    const item = document.createElement("a");
                    item.href = fn.href;
                    item.className = "block px-4 py-2 text-sm text-gray-700 hover:bg-gray-50";
                    item.textContent = fn.label;
                    accountMenu.appendChild(item);
                });
            }

            accountBtn.addEventListener("click", function () {
                accountMenu.classList.toggle("hidden");
            });
            document.addEventListener("click", function (e) {
                if (!accountEl.contains(e.target)) {
                    accountMenu.classList.add("hidden");
                }
            });

            logoutLink.addEventListener("click", function (e) {
                e.preventDefault();
                localStorage.removeItem("gtl.app.session");
                window.location.href = "/app/index.jsp";
            });
        } else {
            loginLink.classList.remove("hidden");
        }
    })();
</script>
