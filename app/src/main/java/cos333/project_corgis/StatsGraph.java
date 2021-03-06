package cos333.project_corgis;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class StatsGraph extends AppCompatActivity {

    // All the Drinks for this session.
    private ArrayList<Drink> log;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_graph);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        log = intent.getParcelableArrayListExtra("session");

        // Set title to include date
        String date = formatDate(log.get(0).time);
        setTitle(String.format(getResources().getString(R.string.graph_title), date));

        populateText();

        populateGraph();

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // Formats the given time (from System.currentTimeMillis) as a date String
    public String formatDate(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.date_format));
        Date resultDate = new Date(time);
        return sdf.format(resultDate);
    }

    // Populates the text views with statistics
    public void populateText() {
        // Length of session
        long first = log.get(0).time;
        long last = log.get(log.size() - 1).time;
        long diff = last - first;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        String timedelta = getResources().getString(R.string.length_format,
                hours, minutes, seconds);

        TextView length = (TextView) findViewById(R.id.length_text);
        length.setText(timedelta);

        // Number of drinks
        double total = 0;
        double highest = 0;
        for (Drink drink : log) {
            total += drink.amount;
            // and highest bac
            if (drink.bac > highest) {
                highest = drink.bac;
            }
        }
        String numDrinks = String.format(getResources().getString(R.string.drinks_label), total);
        TextView drinks = (TextView) findViewById(R.id.num_drink_text);
        drinks.setText(numDrinks);

        // Average drinks per hour
        double hoursDec = diff / (double) 3600000; // magic number to turn millis to hours
        double avg = total / hoursDec;
        String avgText = getResources().getString(R.string.average_format, avg);
        TextView avgDrinks = (TextView) findViewById(R.id.avg_drink_text);
        avgDrinks.setText(avgText);

        // Highest bac
        TextView maxBac = (TextView) findViewById(R.id.highest_bac_text);
        String max = getResources().getString(R.string.max_bac_format, highest);
        maxBac.setText(max);
    }

    // Uses the log data to fill in the graph
    public void populateGraph() {
        int numData = log.size();
        DataPoint[] drinks = new DataPoint[numData];
        double sumDrinks = 0;
        DataPoint[] bac = new DataPoint[numData];
        double max = 0;
        for (int i = 0; i < numData; i++) {
            Drink drink = log.get(i);
            Date date = new Date(drink.time);

            // Running total of drinks in the night
            sumDrinks += drink.amount;
            DataPoint drinkPoint = new DataPoint(date, sumDrinks);
            //DataPoint drinkPoint = new DataPoint(drink.time, sumDrinks);
            drinks[i] = drinkPoint;

            // BAC level at time of drink
            // TODO: include bac decay
            DataPoint bacPoint = new DataPoint(date, drink.bac);
            //DataPoint bacPoint = new DataPoint(i, drink.bac);
            if (drink.bac > max) {
                max = drink.bac;
            }
            bac[i] = bacPoint;
        }

        // Initialize graphs
        GraphView drinkGraph = (GraphView) findViewById(R.id.drinkGraph);
        LineGraphSeries<DataPoint> drinkSeries = new LineGraphSeries<>(drinks);
        drinkSeries.setColor(getResources().getColor(R.color.graph_green));
        drinkGraph.addSeries(drinkSeries);
        drinkGraph.setTitle("Drinks");
        drinkGraph.setTitleColor(getResources().getColor(android.R.color.white));
        GridLabelRenderer drinkGrid = drinkGraph.getGridLabelRenderer();
        drinkGrid.setHorizontalLabelsColor(getResources().getColor(android.R.color.white));
        drinkGrid.setVerticalLabelsColor(getResources().getColor(android.R.color.white));
        drinkGrid.reloadStyles();

        GraphView bacGraph = (GraphView) findViewById(R.id.bacGraph);
        LineGraphSeries<DataPoint> bacSeries = new LineGraphSeries<>(bac);
        bacSeries.setColor(getResources().getColor(R.color.graph_orange));
        bacGraph.addSeries(bacSeries);
        bacGraph.setTitle("BAC");
        bacGraph.setTitleColor(getResources().getColor(android.R.color.white));
        GridLabelRenderer bacGrid = bacGraph.getGridLabelRenderer();
        bacGrid.setHorizontalLabelsColor(getResources().getColor(android.R.color.white));
        bacGrid.setVerticalLabelsColor(getResources().getColor(android.R.color.white));
        bacGrid.reloadStyles();

        // set date label formatter
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        DateAsXAxisLabelFormatter formatterDrink = new DateAsXAxisLabelFormatter(getApplicationContext(), sdf) {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    return super.formatLabel(value, isValueX);
                } else {
                    return String.format("%.1f", value);
                }
            }
        };
        DateAsXAxisLabelFormatter formatterBac = new DateAsXAxisLabelFormatter(getApplicationContext(), sdf) {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    return super.formatLabel(value, isValueX);
                } else {
                    return String.format("%.3f", value);
                }
            }
        };
        drinkGraph.getGridLabelRenderer().setLabelFormatter(formatterDrink);
        drinkGraph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space
        bacGraph.getGridLabelRenderer().setLabelFormatter(formatterBac);
        bacGraph.getGridLabelRenderer().setNumHorizontalLabels(4); // only 4 because of the space

        // set manual x bounds to have nice steps
        drinkGraph.getViewport().setMinX(log.get(0).time-60000);
        drinkGraph.getViewport().setMaxX(log.get(numData - 1).time+60000);
        drinkGraph.getViewport().setXAxisBoundsManual(true);
        bacGraph.getViewport().setMinX(log.get(0).time-60000);
        bacGraph.getViewport().setMaxX(log.get(numData - 1).time+60000);
        bacGraph.getViewport().setXAxisBoundsManual(true);

        // Set Y axis bounds
        drinkGraph.getViewport().setMinY(0);
        drinkGraph.getViewport().setMaxY(sumDrinks);
        drinkGraph.getViewport().setYAxisBoundsManual(true);
        bacGraph.getViewport().setMinY(0);
        bacGraph.getViewport().setMaxY(max);
        bacGraph.getViewport().setYAxisBoundsManual(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "StatsGraph Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://cos333.project_corgis/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "StatsGraph Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://cos333.project_corgis/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
