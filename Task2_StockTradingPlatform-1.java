import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;

public class Task2_StockTradingPlatform {

    // ─── Stock Class ─────────────────────────────────────────────────────────
    static class Stock {
        String symbol, name;
        double price;
        List<Double> priceHistory = new ArrayList<>();

        Stock(String symbol, String name, double price) {
            this.symbol = symbol; this.name = name; this.price = price;
            priceHistory.add(price);
        }

        void updatePrice() {
            double change = (Math.random() * 10 - 5); // -5% to +5%
            price = Math.max(1, price + change);
            price = Math.round(price * 100.0) / 100.0;
            priceHistory.add(price);
            if (priceHistory.size() > 10) priceHistory.remove(0);
        }

        String getSymbol()             { return symbol; }
        String getName()               { return name; }
        double getPrice()              { return price; }
        List<Double> getPriceHistory() { return priceHistory; }
    }

    // ─── Portfolio Holding ────────────────────────────────────────────────────
    static class Holding {
        String symbol;
        int quantity;
        double avgBuyPrice;

        Holding(String symbol, int qty, double price) {
            this.symbol = symbol; this.quantity = qty; this.avgBuyPrice = price;
        }
    }

    // ─── Transaction ─────────────────────────────────────────────────────────
    static class Transaction {
        String type, symbol, date;
        int qty;
        double price;

        Transaction(String type, String symbol, int qty, double price) {
            this.type = type; this.symbol = symbol; this.qty = qty; this.price = price;
            this.date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
        }

        public String toString() {
            return String.format("[%s] %s %d x %s @ $%.2f", date, type, qty, symbol, price);
        }
    }

    // ─── Global State ─────────────────────────────────────────────────────────
    static Map<String, Stock> market = new LinkedHashMap<>();
    static Map<String, Holding> portfolio = new HashMap<>();
    static List<Transaction> transactions = new ArrayList<>();
    static double balance = 10000.00;
    static Scanner sc = new Scanner(System.in);
    static final String FILE = "portfolio.txt";

    public static void main(String[] args) {
        initMarket();
        loadPortfolio();
        System.out.println("==========================================");
        System.out.println("       STOCK TRADING PLATFORM            ");
        System.out.println("==========================================");
        System.out.printf("💰 Starting Balance: $%.2f%n", balance);

        boolean running = true;
        while (running) {
            refreshPrices();
            System.out.println("\n--- MENU ---");
            System.out.println("1. View Market");
            System.out.println("2. Buy Stock");
            System.out.println("3. Sell Stock");
            System.out.println("4. View Portfolio");
            System.out.println("5. View Price History");
            System.out.println("6. Transaction History");
            System.out.println("7. Exit");
            System.out.print("Choose option: ");

            switch (readInt()) {
                case 1: viewMarket(); break;
                case 2: buyStock(); break;
                case 3: sellStock(); break;
                case 4: viewPortfolio(); break;
                case 5: viewPriceHistory(); break;
                case 6: viewTransactions(); break;
                case 7: running = false; savePortfolio(); System.out.println("Goodbye! 💾 Saved."); break;
                default: System.out.println("Invalid option.");
            }
        }
    }

    static void initMarket() {
        market.put("AAPL", new Stock("AAPL", "Apple Inc.",       178.50));
        market.put("GOOGL",new Stock("GOOGL","Alphabet Inc.",    142.30));
        market.put("MSFT", new Stock("MSFT", "Microsoft Corp.",  415.20));
        market.put("AMZN", new Stock("AMZN", "Amazon.com Inc.",  185.75));
        market.put("TSLA", new Stock("TSLA", "Tesla Inc.",       175.60));
        market.put("NVDA", new Stock("NVDA", "NVIDIA Corp.",     875.40));
    }

    static void refreshPrices() {
        for (Stock s : market.values()) s.updatePrice();
    }

    static void viewMarket() {
        System.out.println("\n====== LIVE MARKET ======");
        System.out.printf("%-6s %-20s %10s%n", "Symbol", "Company", "Price");
        System.out.println("---------------------------------------");
        for (Stock s : market.values())
            System.out.printf("%-6s %-20s $%9.2f%n", s.getSymbol(), s.getName(), s.getPrice());
    }

    static void buyStock() {
        viewMarket();
        System.out.print("Enter symbol to buy: ");
        String sym = sc.nextLine().trim().toUpperCase();
        Stock s = market.get(sym);
        if (s == null) { System.out.println("Invalid symbol."); return; }

        System.out.printf("Price: $%.2f | Your balance: $%.2f%n", s.getPrice(), balance);
        System.out.print("Quantity to buy: ");
        int qty = readInt();
        if (qty <= 0) { System.out.println("Invalid quantity."); return; }

        double cost = s.getPrice() * qty;
        if (cost > balance) { System.out.printf("Insufficient funds. Need $%.2f%n", cost); return; }

        balance -= cost;
        Holding h = portfolio.getOrDefault(sym, new Holding(sym, 0, 0));
        double newAvg = (h.avgBuyPrice * h.quantity + cost) / (h.quantity + qty);
        h.quantity += qty;
        h.avgBuyPrice = newAvg;
        portfolio.put(sym, h);
        transactions.add(new Transaction("BUY", sym, qty, s.getPrice()));
        savePortfolio();
        System.out.printf("✅ Bought %d x %s for $%.2f | Balance: $%.2f%n", qty, sym, cost, balance);
    }

    static void sellStock() {
        if (portfolio.isEmpty()) { System.out.println("Portfolio is empty."); return; }
        viewPortfolio();
        System.out.print("Enter symbol to sell: ");
        String sym = sc.nextLine().trim().toUpperCase();
        Holding h = portfolio.get(sym);
        if (h == null) { System.out.println("You don't own this stock."); return; }

        System.out.print("Quantity to sell (max " + h.quantity + "): ");
        int qty = readInt();
        if (qty <= 0 || qty > h.quantity) { System.out.println("Invalid quantity."); return; }

        Stock s = market.get(sym);
        double revenue = s.getPrice() * qty;
        balance += revenue;
        h.quantity -= qty;
        if (h.quantity == 0) portfolio.remove(sym);
        transactions.add(new Transaction("SELL", sym, qty, s.getPrice()));
        savePortfolio();
        System.out.printf("✅ Sold %d x %s for $%.2f | Balance: $%.2f%n", qty, sym, revenue, balance);
    }

    static void viewPortfolio() {
        System.out.printf("%n====== PORTFOLIO | Balance: $%.2f ======%n", balance);
        if (portfolio.isEmpty()) { System.out.println("No holdings."); return; }
        double totalValue = balance;
        System.out.printf("%-6s %5s %10s %10s %10s %8s%n", "Symbol","Qty","Avg Buy","Curr Price","Value","P/L");
        System.out.println("--------------------------------------------------------------");
        for (Holding h : portfolio.values()) {
            Stock s = market.get(h.symbol);
            double currVal = s.getPrice() * h.quantity;
            double pl = currVal - h.avgBuyPrice * h.quantity;
            totalValue += currVal;
            System.out.printf("%-6s %5d %10.2f %10.2f %10.2f %+8.2f%n",
                h.symbol, h.quantity, h.avgBuyPrice, s.getPrice(), currVal, pl);
        }
        System.out.printf("Total Portfolio Value: $%.2f%n", totalValue);
    }

    static void viewPriceHistory() {
        System.out.print("Enter symbol: ");
        String sym = sc.nextLine().trim().toUpperCase();
        Stock s = market.get(sym);
        if (s == null) { System.out.println("Invalid symbol."); return; }
        System.out.println("\n-- Price History: " + sym + " --");
        List<Double> hist = s.getPriceHistory();
        for (int i = 0; i < hist.size(); i++)
            System.out.printf("  Tick %-3d: $%.2f%n", i, hist.get(i));
    }

    static void viewTransactions() {
        if (transactions.isEmpty()) { System.out.println("No transactions yet."); return; }
        System.out.println("\n-- Transaction History --");
        for (Transaction t : transactions) System.out.println(t);
    }

    static void savePortfolio() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println("BALANCE:" + balance);
            for (Holding h : portfolio.values())
                pw.println("HOLD:" + h.symbol + ":" + h.quantity + ":" + h.avgBuyPrice);
            for (Transaction t : transactions)
                pw.println("TXN:" + t.type + ":" + t.symbol + ":" + t.qty + ":" + t.price + ":" + t.date);
        } catch (IOException e) { System.out.println("Save error: " + e.getMessage()); }
    }

    static void loadPortfolio() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(":");
                if (p[0].equals("BALANCE")) balance = Double.parseDouble(p[1]);
                else if (p[0].equals("HOLD"))
                    portfolio.put(p[1], new Holding(p[1], Integer.parseInt(p[2]), Double.parseDouble(p[3])));
                else if (p[0].equals("TXN")) {
                    Transaction t = new Transaction(p[1], p[2], Integer.parseInt(p[3]), Double.parseDouble(p[4]));
                    if (p.length > 5) t.date = p[5];
                    transactions.add(t);
                }
            }
            System.out.println("📂 Portfolio loaded.");
        } catch (IOException e) { System.out.println("Load error: " + e.getMessage()); }
    }

    static int readInt() {
        try { return Integer.parseInt(sc.nextLine().trim()); }
        catch (NumberFormatException e) { return -1; }
    }
}
