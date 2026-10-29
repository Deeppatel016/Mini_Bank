package model;

import java.io.Serializable;

public class Customer implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String mobile;
    private Address address;

    private final String customerId;
    private static long customerCount = 100;

    public static class Address implements Serializable {
        private static final long serialVersionUID = 1L;
        private String line;
        private String city;
        private String pincode;

        public Address(String line, String city, String pincode) {
            this.line = line;
            this.city = city;
            this.pincode = pincode;
        }

        public String getLine() {
            return line;
        }

        public String getCity() {
            return city;
        }

        public String getPincode() {
            return pincode;
        }
    }

    private static synchronized String generateCustomerId() {
        customerCount++;
        return "CUST" + customerCount;
    }

    public static synchronized void updateCustomerCount(String custId) {
        if (custId != null) {
            String numStr = custId.replaceAll("\\D+", "");
            if (!numStr.isEmpty()) {
                try {
                    long num = Long.parseLong(numStr);
                    if (num > customerCount) {
                        customerCount = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public Customer(String name, String email, String mobile) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.customerId = generateCustomerId();
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public Customer clone() {
        try {
            return (Customer) super.clone();
        } catch (CloneNotSupportedException e) {
            Customer copy = new Customer(this.name, this.email, this.mobile);
            copy.address = this.address;
            return copy;
        }
    }
}
