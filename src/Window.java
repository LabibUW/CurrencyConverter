import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.event.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.net.URL;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

public class Window {

    public static JFrame frame;
    public static JPanel panel;
    private static HttpURLConnection connection;
    public static Object selected;
    public static Object selected2;
    private static Connection SQLConnection;
    public static String MYSQL_ADDRESS = "YOUR_ADDRESS";
    public static String MYSQL_USER = "YOUR_USER";
    public static String MYSQL_PASSWORD = "YOUR_PASSWORD";

//    Get your free API Key here: https://www.currencyconverterapi.com/
    public static String CURRENCYCONVERT_API_KEY = "YOUR_API_KEY";


    public static void main(String[] args) {
        //Create JFrame
        frame = new JFrame("Currency Converter");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GridBagConstraints gbc = new GridBagConstraints();


        //Create various buttons/elements
        JLabel label1 = new JLabel("Convert  ");
        JLabel label2 = new JLabel("to:");
        JLabel spacer = new JLabel("          ");
        JLabel result = new JLabel();

        JButton b = new JButton("Convert!");
        JButton history = new JButton("History");


        JTextField textField = new JTextField("", 3);

        // Currency List

        // While API is Enabled
//        StringBuffer responseCurrency = requestCurrencyAPI();
//        ArrayList<String> currencies = parse(responseCurrency.toString());
//        System.out.println(currencies);
//        JComboBox<String> cb1 = new JComboBox<String>(currencies.toArray(new String[0]));
//        JComboBox<String> cb2 = new JComboBox<String>(currencies.toArray(new String[0]));

        // While API is Disabled
        String[] choices = { "USD","CAD", "PHP", "AUD"};
        JComboBox<String> cb1 = new JComboBox<String>(choices);
        JComboBox<String> cb2 = new JComboBox<String>(choices);

        // Create Panel and add all elements to it
        panel = new JPanel();
        panel.add(label1);
        panel.add(textField);
        panel.add(cb1);
        panel.add(label2);
        panel.add(cb2);
        panel.add(b);
//        panel.add(spacer);
        panel.add(result);
        panel.add(history);
        createConnection();


        // Convert Button Pressed Listener
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selected = cb1.getSelectedItem();
                System.out.println(selected.toString());
                selected2 = cb2.getSelectedItem();
                System.out.println(selected2.toString());
                // Usage of API to convert currency
                StringBuffer responseContent = requestConvertAPI(selected.toString(), selected2.toString());
                // Double to a 0.00 value
                DecimalFormat df = new DecimalFormat("#.##");
                double balance = 0;
                double conversion = parse(responseContent.toString(), selected.toString(), selected2.toString());
                try {
                    // Convert Number from long number to #.##
                    balance = Double.valueOf(df.format(Double.parseDouble(textField.getText()) * conversion));
                    // Set Text of conversion
                    result.setText("The Result is: " + balance);
                } catch (NumberFormatException nfe) {
                    // Return an error if textbox does not have a number
                    result.setText("There is an invalid value in the textbox.");
                }


                try {
                    // Create new entry in SQL
                    Connection SQLConnection = DriverManager.getConnection(MYSQL_ADDRESS, MYSQL_USER, MYSQL_PASSWORD);
                    Statement statement = SQLConnection.createStatement();
                    statement.execute("INSERT INTO Search VALUES ('"+ selected.toString() +"', '" + selected2.toString() + "', " + Double.parseDouble(textField.getText()) + ", " + balance +");");
                } catch (SQLException er) {
                    er.printStackTrace();
                }



            }
        });

        // History Button Pressed Listener
        history.addActionListener((new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    history();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }

            }
        }));

//        String cbvalue1 = Arrays.toString((String[])cb1.getSelectedItem());
//        String cbvalue2 = Arrays.toString((String[])cb2.getSelectedItem());


        frame.add(panel);

        frame.setSize(320, 300);

        frame.setVisible(true);
    }

    private static void createConnection() {
        String url = MYSQL_ADDRESS;
        String uname = MYSQL_USER;
        String password = MYSQL_PASSWORD;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            SQLConnection = DriverManager.getConnection(url, uname, password);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public static void history() throws SQLException {
        // Creation of History Panel
        JPanel historyPanel = new JPanel();
        Button btn = new Button("Go Back!");

        // Referencing all SQL rows
        Statement statement = SQLConnection.createStatement();
        ResultSet result = statement.executeQuery("select * from Search");

        String[] columns = {"FROM", "TO", "START", "END"};
        JTable table = new JTable();


        // Adding onto the JTable
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0);

        tableModel.addRow(columns);
        while (result.next()) {
            String startCurr = result.getString("START_CURR");
            String endCurr = result.getString("END_CURR");
            String startBal = result.getString("StartBalance");
            String endBal = result.getString("EndBalance");

            // create a single array of one row's worth of data
            String[] data = { startCurr, endCurr, startBal, endBal } ;

            // and add this row of data into the table model
            tableModel.addRow(data);
        }



        table.setModel(tableModel);

        // Adding elements to the History Panel
        historyPanel.add(btn);
        historyPanel.add(table);
        frame.add(historyPanel);
        panel.setVisible(false);

        // Go Back Button Pressed Listner
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Close History Panel and show Conversion Panel
                historyPanel.setVisible(false);
                panel.setVisible(true);
            }
        });
    }

    // Reference Currency Conversion API
    public static StringBuffer requestConvertAPI(String baseCurrency, String newCurrency){
        BufferedReader reader;
        String line;
        StringBuffer responseContent = new StringBuffer();
        try{

            // Connect to the API
            URL url = new URL("https://free.currconv.com/api/v7/convert?q="+baseCurrency + "_" + newCurrency+"&compact=ultra&apiKey=" + CURRENCYCONVERT_API_KEY);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
//            System.out.println(status);

            if (status > 299) {
                // Throw error
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                // Read API output
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        return responseContent;
    }

    // Access Local Currency List API
//
//    Linked Here: https://github.com/LabibUW/currencyconverterapi
//

    public static StringBuffer requestCurrencyAPI(){
        BufferedReader reader;
        String line;
        StringBuffer responseContent = new StringBuffer();
        try{
            // Connect to the API
            URL url = new URL("http://localhost:3000/currencylist");
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();


            if (status > 299) {
                // Throw Error
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                while((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
                reader.close();
            } else {
                // Read APi Output
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while((line = reader.readLine()) != null) {
                    responseContent.append(line);
                }
            }

            parse(responseContent.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        return responseContent;
    }

    // Parse Currency Conversion API Response Body
    public static double parse(String responseBody, String baseCurrency, String newCurrency) {
        JSONArray responses = new JSONArray("["+responseBody+"]");
        JSONObject response = responses.getJSONObject(0);
        double conversion = response.getDouble(baseCurrency + "_" + newCurrency);

        return conversion;
    }

    // Parse Local Currency List API Response Body
    public static ArrayList<String> parse(String responseBody) {
        JSONArray currencies = new JSONArray(responseBody);
        ArrayList<String> currencyList= new ArrayList<>();
        for(int i = 0; i < currencies.length(); i++) {
            JSONObject currency = currencies.getJSONObject(i);
            currencyList.add(currency.getString("abbreviation"));

        }

        return currencyList;
    }
}
