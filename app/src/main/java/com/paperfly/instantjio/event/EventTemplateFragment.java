package com.paperfly.instantjio.event;


import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paperfly.instantjio.R;
import com.paperfly.instantjio.contact.ContactsChooserActivity;
import com.paperfly.instantjio.group.GroupsChooserActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Semaphore;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EventTemplateFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EventTemplateFragment extends Fragment implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHOSEN_GROUPS = "CHOSEN_GROUPS";
    private static final String CHOSEN_CONTACTS = "CHOSEN_CONTACTS";
    private static final int GROUPS_CHOOSER_RC = 0;
    private static final int CONTACTS_CHOOSER_RC = 1;
    private static final LatLngBounds BOUNDS_GREATER_SYDNEY = new LatLngBounds(
            new LatLng(-34.041458, 150.790100), new LatLng(-33.682247, 151.383362));
    private final String TAG = EventTemplateFragment.class.getCanonicalName();
    protected GoogleApiClient mGoogleApiClient;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private ArrayList<String> mChosenGroups;
    private HashMap<String, String> mChosenContacts;
    private DatePickerDialog startDatePickerDialog;
    private DatePickerDialog endDatePickerDialog;
    private TimePickerDialog startTimePickerDialog;
    private TimePickerDialog endTimePickerDialog;
    private PlaceAutocompleteAdapter mAdapter;

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(TAG, "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            // Get the Place object from the buffer.
            final Place place = places.get(0);

            // Display the third party attributions if set.
            final CharSequence thirdPartyAttribution = places.getAttributions();

            Log.i(TAG, "Place details received: " + place.getName());

            places.release();
        }
    };
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            Log.i(TAG, "Autocomplete item selected: " + primaryText);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Log.i(TAG, "Called getPlaceById to get Place details for " + placeId);
        }
    };

    public EventTemplateFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EventTemplateFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EventTemplateFragment newInstance(String param1, String param2) {
        EventTemplateFragment fragment = new EventTemplateFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_event_template, container, false);
        mChosenGroups = new ArrayList<>();
        mChosenContacts = new HashMap<>();

        initEventListeners(root);

        Button startTime = (Button) root.findViewById(R.id.event_template_start_time);
        startTime.setInputType(InputType.TYPE_NULL);
        Button endTime = (Button) root.findViewById(R.id.event_template_end_time);
        endTime.setInputType(InputType.TYPE_NULL);

        setTimeField(root);

        // Construct a GoogleApiClient for the {@link Places#GEO_DATA_API} using AutoManage
        // functionality, which automatically sets up the API client to handle Activity lifecycle
        // events. If your activity does not extend FragmentActivity, make sure to call connect()
        // and disconnect() explicitly.
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
//                .enableAutoManage(this, 0 /* clientId */, this)
                .addApi(Places.GEO_DATA_API)
                .build();

        // Retrieve the AutoCompleteTextView that will display Place suggestions.
        AutoCompleteTextView mAutocompleteView = (AutoCompleteTextView)
                root.findViewById(R.id.event_template_location);

        // Register a listener that receives callbacks when a suggestion has been selected
        mAutocompleteView.setOnItemClickListener(mAutocompleteClickListener);

        // Set up the adapter that will retrieve suggestions from the Places Geo Data API that cover
        // the entire world.
        mAdapter = new PlaceAutocompleteAdapter(getContext(), mGoogleApiClient, BOUNDS_GREATER_SYDNEY,
                null);
        mAutocompleteView.setAdapter(mAdapter);
        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.event_custom_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.event_create) {
            createTemplate();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    void initEventListeners(final View view) {
        view.findViewById(R.id.event_template_invite_group).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGroupsChooser();
            }
        });

        view.findViewById(R.id.event_template_invite_friend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startContactsChooser();
            }
        });

        view.findViewById(R.id.event_template_create).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTemplate();
            }
        });
    }

    public void startGroupsChooser() {
        final Intent intent = new Intent(getContext(), GroupsChooserActivity.class);

        if (mChosenGroups != null) {
            intent.putExtra(CHOSEN_GROUPS, mChosenGroups);
        }

        startActivityForResult(intent, GROUPS_CHOOSER_RC);
    }

    public void startContactsChooser() {
        final Intent intent = new Intent(getContext(), ContactsChooserActivity.class);

        if (mChosenContacts != null)
            intent.putExtra(CHOSEN_CONTACTS, mChosenContacts);

        startActivityForResult(intent, CONTACTS_CHOOSER_RC);
    }

    private void setTimeField(View root) {
        final Button startTime = (Button) root.findViewById(R.id.event_template_start_time);
        final Button endTime = (Button) root.findViewById(R.id.event_template_end_time);
        final SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        startTime.setOnClickListener(this);
        endTime.setOnClickListener(this);

        Calendar newCalendar = Calendar.getInstance();
        startTime.setText(timeFormatter.format(newCalendar.getTime()));
        endTime.setText(timeFormatter.format(newCalendar.getTime()));

        startTimePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar newTime = Calendar.getInstance();

                newTime.set(0, 0, 0, hourOfDay, minute, 0);
                startTime.setText(timeFormatter.format(newTime.getTime()));
            }
        }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), false);

        endTimePickerDialog = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Calendar newTime = Calendar.getInstance();

                newTime.set(0, 0, 0, hourOfDay, minute, 0);
                endTime.setText(timeFormatter.format(newTime.getTime()));
            }
        }, newCalendar.get(Calendar.HOUR_OF_DAY), newCalendar.get(Calendar.MINUTE), false);
    }

    @Override
    public void onClick(View view) {
        if (view == view.findViewById(R.id.event_template_start_time)) {
            startTimePickerDialog.show();
        } else if (view == view.findViewById(R.id.event_template_end_time)) {
            endTimePickerDialog.show();
        }
        view.clearFocus();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GROUPS_CHOOSER_RC) {
            if (resultCode == Activity.RESULT_OK) {
                mChosenGroups = MergeList(mChosenGroups, (ArrayList<String>) data.getSerializableExtra(CHOSEN_GROUPS));

                for (int i = 0; i < mChosenGroups.size(); ++i) {
                    Log.i(TAG, mChosenGroups.get(i));
                }
            }
        } else if (requestCode == CONTACTS_CHOOSER_RC) {
            if (resultCode == Activity.RESULT_OK) {
                mChosenContacts = MergeMap(mChosenContacts, (HashMap<String, String>) data.getSerializableExtra(CHOSEN_CONTACTS));

                for (HashMap.Entry<String, String> entry : mChosenContacts.entrySet()) {
                    Log.i(TAG, entry.getValue());
                }
            }
        }
    }

    // Merge two maps from right to left
    private HashMap<String, String> MergeMap(HashMap<String, String> left, HashMap<String, String> right) {
        final HashMap<String, String> temp = new HashMap<>();
        // Remove deselected chosenContacts here
        for (HashMap.Entry<String, String> entry : left.entrySet()) {
            if (right.containsKey(entry.getKey())) {
                temp.put(entry.getKey(), entry.getValue());
            }
        }
        // Merge them
        for (HashMap.Entry<String, String> entry : right.entrySet()) {
            temp.put(entry.getKey(), entry.getValue());
        }
        return temp;
    }

    private ArrayList<String> MergeList(ArrayList<String> left, ArrayList<String> right) {
        final ArrayList<String> temp = new ArrayList<>();
        // Remove deselected chosenContacts here
        for (int i = 0; i < left.size(); ++i) {
            if (right.contains(left.get(i))) {
                temp.add(left.get(i));
            }
        }
        // Merge them
        for (int i = 0; i < right.size(); ++i) {
            if (!temp.contains(right.get(i))) {
                temp.add(right.get(i));
            }
        }
        return temp;
    }

    /**
     * Called when the Activity could not connect to Google Play services and the auto manager
     * could resolve the error automatically.
     * In this case the API is not available and notify the user.
     *
     * @param connectionResult can be inspected to determine the cause of the failure
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.e(TAG, "onConnectionFailed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

        // TODO(Developer): Check error code and notify the user of error state and resolution.
        Toast.makeText(getContext(),
                "Could not connect to Google API Client: Error " + connectionResult.getErrorCode(),
                Toast.LENGTH_SHORT).show();
    }

    private void createTemplate() {
        new CreateTemplateTask(getView()).execute();

        getActivity().finish();
    }

    private class CreateTemplateTask extends AsyncTask<Void, Void, Void> {
        private Event template;

        public CreateTemplateTask(final View root) {
            super();
            template = new Event();
            getUIData(root);
        }

        private void getUIData(final View view) {
            final EditText title = (EditText) view.findViewById(R.id.event_template_title);
            final EditText description = (EditText) view.findViewById(R.id.event_template_description);
            final AutoCompleteTextView location = (AutoCompleteTextView) view.findViewById(R.id.event_template_location);
            final Button startTime = (Button) view.findViewById(R.id.event_template_start_time);
            final Button endTime = (Button) view.findViewById(R.id.event_template_end_time);
            template.setTitle(title.getText().toString());
            template.setDescription(description.getText().toString());
            template.setLocation(location.getText().toString());
            template.setStartTime(startTime.getText().toString());
            template.setEndTime(endTime.getText().toString());
        }

        @Override
        protected Void doInBackground(Void... params) {
            final DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groups");
            final DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users");
//            final Firebase ref = new Firebase(getString(R.string.firebase_url));
            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            final String uid = user != null ? user.getUid() : "";
            final DatabaseReference newRef = userRef.child(uid).child("templates").push();
            final Semaphore semaphore = new Semaphore(0);

            // Add invited contacts' indices
            for (HashMap.Entry<String, String> entry : mChosenContacts.entrySet()) {
                template.addInvited(entry.getValue());
            }

            // Add invited group members' indices
            for (int i = 0; i < mChosenGroups.size(); ++i) {
                groupRef.child(mChosenGroups.get(i)).child("members").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                            template.addInvited(postSnapshot.getKey());
                        }

                        semaphore.release();
                    }

                    @Override
                    public void onCancelled(DatabaseError firebaseError) {

                    }
                });
            }

            try {
                semaphore.acquire(mChosenGroups.size());

                template.setHost(uid);
                template.addInvited(uid);

                newRef.setValue(template);
            } catch (InterruptedException e) {
                Log.e(TAG, e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i(TAG, "Successfully created new template");
        }
    }

}
