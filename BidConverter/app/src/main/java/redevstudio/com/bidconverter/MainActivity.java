package redevstudio.com.bidconverter;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private String TAG = MainActivity.class.getSimpleName();
    private static String url, rateValue, baseCurrency, targetCurrency, amountVal,date;
    //Spinners for base and target currencies
    private Spinner baseSpinner, targetSpinner;
    private EditText amountEdit;
    //date Selector
    private TextView selDate;
    private DatePickerDialog.OnDateSetListener setDateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        baseSpinner = (Spinner) findViewById(R.id.baseSpinner);
        targetSpinner = (Spinner) findViewById(R.id.targetSpinner);
        amountEdit = (EditText) findViewById(R.id.amountInput);

        //Populating base & and target currency spinners using an adapter
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.currencies, android.R.layout.simple_spinner_item);
        baseSpinner.setAdapter(adapter);
        baseSpinner.setOnItemSelectedListener(MainActivity.this);
        targetSpinner.setAdapter(adapter);
        targetSpinner.setOnItemSelectedListener(MainActivity.this);

        //Date Selector
        selDate = (TextView) findViewById(R.id.tvDate);

        selDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);



                DatePickerDialog dialog = new DatePickerDialog(
                        MainActivity.this,
                        android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                        setDateListener,
                        year,month,day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                //Setting today as the latest day, since we cannot guess the future's rates!
                dialog.getDatePicker().setMaxDate(new Date().getTime());
                //Fixer.io only provides rates starting from the year 2000
                dialog.getDatePicker().setMinDate(946767600000L);
                dialog.show();
            }
        });

        setDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                month = month + 1;
//                Log.d(TAG, "onDateSet: mm/dd/yyy: " + year + "-" +(day<10?("0"+day):(day)) + "-" + (month<10?("0"+month):(month)));
                date = year + "-" +(month<10?("0"+month):(month)) + "-" + (day<10?("0"+day):(day));
                selDate.setText(date);
            }
        };

        //Convert Button
        final Button convertBtn = (Button) findViewById(R.id.convertBtn);
        convertBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Retrieve the user entered information to calculate the conversion
                baseCurrency = baseSpinner.getSelectedItem().toString();
                targetCurrency = targetSpinner.getSelectedItem().toString();
                amountVal = amountEdit.getText().toString();

                //Some validation rules
                if(baseCurrency.equals(targetCurrency)){
                    Toast.makeText(getApplicationContext(),
                            "You cannot convert the same currency!",
                            Toast.LENGTH_LONG)
                            .show();
                }else if(amountVal.matches("")){
                    Toast.makeText(getApplicationContext(),
                            "Please enter a valid amount to convert!",
                            Toast.LENGTH_LONG)
                            .show();
                }else if(date == null){
                    Toast.makeText(getApplicationContext(),
                            "Please select a valid date!",
                            Toast.LENGTH_LONG)
                            .show();
                }else {
                    //FIXER.IO API URL which contains the all the exchange rates we need
                    url = "http://api.fixer.io/"+date+"?base=" + baseCurrency;
                Log.d("url", url);

                    //Retrieve the data, then parse it
                    new GetExchangeRate().execute();
                }

            }
        });



    }
    //The custom method which calculates the new exchange rates, then display it for the user
    private void calculateConversion(String amount, String rateValue){
        Double convertedValue = Double.parseDouble(amount) * Double.parseDouble(rateValue);
        TextView textView = (TextView) findViewById(R.id.convertTxt);
        String outputTxt = "On " + date + ": \n" + amountVal +" "+baseCurrency + " = "+(double)Math.round(convertedValue * 10000d) / 10000d
                +" "+targetCurrency;
        textView.setText(String.valueOf(outputTxt));
//        Log.d("conversion", String.valueOf(convertedValue));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private class GetExchangeRate extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url);

            Log.e(TAG, "Response from url: " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    // Finding JSONOBject "rates" containing the rate values
                    JSONObject rates = jsonObj.getJSONObject("rates");
                    //Retrieving the value in the JSONObject containing the user's desired target currency
                    rateValue = rates.getString(targetCurrency);
//                    Log.d("rates", rateValue);

                } catch (final JSONException e) {
                    Log.e(TAG, "JSON parse error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "JSON parse error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });

                }
            } else {
                Log.e(TAG, "There was an error retrieving the JSON from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "There was an error retrieving the JSON from server.",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if(amountVal !=null && rateValue != null){
                calculateConversion(amountVal, rateValue);
            }


        }

    }
}







