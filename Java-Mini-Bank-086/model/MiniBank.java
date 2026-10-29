package model;

import exception.BankException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
import service.AccountStore;
import service.BankService;
import service.TransactionProcessor;
import test.MiniBankTest;
import util.BankInfo;
import util.ReportGenerator;
import util.Repository;
import util.StatePersister;

public class MiniBank {

    private static void saveAccounts(Repository<Account> repo, Path dataFile) {
        try {
            if (repo != null && dataFile != null) {
                Account[] all = repo.findAll().toArray(new Account[0]);
                StatePersister.save(all, dataFile);
            }
        } catch (Exception e) {
            System.out.println("Auto-save error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        BankInfo bank = new BankInfo("MiniBank", "Anand");

        System.out.println("Welcome to " + bank.name());
        System.out.println("Branch: " + bank.branch());

        Path dataFile = Path.of("data", "accounts.ser");
        System.out.println("Data file location: " + dataFile.toAbsolutePath());
        Path logsDir = Path.of("logs");
        Path reportFile = Path.of("reports", "summary.txt");

        Repository<Account> repo = new Repository<>();
        Account[] loadedAccounts = StatePersister.load(dataFile);
        if (loadedAccounts != null && loadedAccounts.length > 0) {
            repo.saveAll(loadedAccounts);
            Account.updateCounterFromLoadedAccounts(loadedAccounts);
            System.out.println("Loaded " + loadedAccounts.length + " accounts from storage");
        } else {
            Account a1 = new SavingsAccount("Rahul", 10000, 1000);
            Account a2 = new CurrentAccount("Rohan", 5000, 2000);
            Account a3 = new FixedDepositAccount("Jimit", 20000);
            repo.saveAll(a1, a2, a3);
            Account.updateCounterFromLoadedAccounts(repo.findAll().toArray(new Account[0]));
            saveAccounts(repo, dataFile);
        }

        // Auto-save on process termination/Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveAccounts(repo, dataFile);
        }));

        BankService service = new BankService(repo);

        AccountStore store = new AccountStore();
        for (Account a : repo.findAll()) {
            store.add(a);
        }
        System.out.println("Accounts sorted by balance:");
        for (Account a : store.sortedByBalance()) {
            System.out.println(a.getAccountNumber() + " - " + a.getOwnerName() + " - " + a.getBalance());
        }

        Double[] balances = new Double[repo.findAll().size()];
        ArrayList<Account> currentList = repo.findAll();
        for (int i = 0; i < currentList.size(); i++) {
            balances[i] = (double) currentList.get(i).getBalance();
        }
        System.out.println("Average balance: " + Repository.average(balances));

        while (true) {
            System.out.println("\n1. Open Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Statement");
            System.out.println("6. List Accounts");
            System.out.println("7. Batch Transactions");
            System.out.println("8. Generate Report");
            System.out.println("9. Run Tests");
            System.out.println("10. Exit");
            System.out.print("Enter choice: ");

            if (!sc.hasNextInt()) {
                break;
            }
            int choice = sc.nextInt();
            sc.nextLine();

            try {
                if (choice == 1) {
                    System.out.print("Enter type (SAVINGS/CURRENT/FD): ");
                    String type = sc.nextLine();
                    System.out.print("Enter owner name: ");
                    String name = sc.nextLine();
                    System.out.print("Enter initial balance: ");
                    long balance = sc.nextLong();
                    long extra = 0;
                    if (type.equalsIgnoreCase("SAVINGS") || type.equalsIgnoreCase("CURRENT")) {
                        System.out.print("Enter limit or min balance: ");
                        extra = sc.nextLong();
                    }
                    sc.nextLine();
                    Account acc = service.openAccount(type, name, balance, extra);
                    saveAccounts(repo, dataFile);
                    System.out.println("Created account: " + acc.getAccountNumber());
                } else if (choice == 2) {
                    System.out.print("Enter account number: ");
                    String accNum = sc.nextLine();
                    System.out.print("Enter amount: ");
                    long amount = sc.nextLong();
                    sc.nextLine();
                    service.deposit(accNum, amount);
                    saveAccounts(repo, dataFile);
                    System.out.println("Deposit successful");
                } else if (choice == 3) {
                    System.out.print("Enter account number: ");
                    String accNum = sc.nextLine();
                    System.out.print("Enter amount: ");
                    long amount = sc.nextLong();
                    sc.nextLine();
                    service.withdraw(accNum, amount);
                    saveAccounts(repo, dataFile);
                    System.out.println("Withdrawal successful");
                } else if (choice == 4) {
                    System.out.print("Enter source account: ");
                    String from = sc.nextLine();
                    System.out.print("Enter target account: ");
                    String to = sc.nextLine();
                    System.out.print("Enter amount: ");
                    long amount = sc.nextLong();
                    sc.nextLine();
                    service.transfer(from, to, amount);
                    saveAccounts(repo, dataFile);
                    System.out.println("Transfer successful");
                } else if (choice == 5) {
                    System.out.print("Enter account number: ");
                    String accNum = sc.nextLine();
                    System.out.println(service.statement(accNum));
                } else if (choice == 6) {
                    for (Account a : service.listAccounts()) {
                        System.out.println(a);
                    }
                } else if (choice == 7) {
                    TransactionProcessor processor = new TransactionProcessor();
                    for (Account a : service.listAccounts()) {
                        processor.registerAccount(a);
                    }
                    ArrayList<Account> accounts = service.listAccounts();
                    if (!accounts.isEmpty()) {
                        Account target = accounts.get(0);
                        for (int i = 0; i < 5; i++) {
                            processor.submit(() -> {
                                try {
                                    target.deposit(50);
                                    System.out.println("Batch deposit completed for " + target.getAccountNumber());
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }
                            });
                        }
                    }
                    processor.stop();
                    saveAccounts(repo, dataFile);
                    System.out.println("Batch processing completed");
                } else if (choice == 8) {
                    ReportGenerator.generateReport(logsDir, reportFile);
                    if (Files.exists(reportFile)) {
                        System.out.println(Files.readString(reportFile));
                    }
                } else if (choice == 9) {
                    MiniBankTest.runAllTests();
                } else if (choice == 10) {
                    saveAccounts(repo, dataFile);
                    System.out.println("Accounts saved. Exiting...");
                    break;
                } else {
                    System.out.println("Invalid option");
                }
            } catch (BankException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }

        saveAccounts(repo, dataFile);
        sc.close();
    }
}
