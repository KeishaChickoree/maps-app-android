/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 *
 */

package com.esri.android.mapsapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.support.design.widget.FloatingActionButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.mapsapp.account.AccountManager;
import com.esri.android.mapsapp.basemaps.BasemapsDialogFragment.BasemapsDialogListener;
import com.esri.android.mapsapp.dialogs.ProgressDialogFragment;
import com.esri.android.mapsapp.location.DirectionsDialogFragment;
import com.esri.android.mapsapp.location.DirectionsDialogFragment.DirectionsDialogListener;
import com.esri.android.mapsapp.location.RoutingDialogFragment;
import com.esri.android.mapsapp.location.RoutingDialogFragment.RoutingDialogListener;
import com.esri.android.mapsapp.tools.Compass;
import com.esri.android.mapsapp.util.TaskExecutor;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.AreaUnit;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.Unit;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.WrapAroundMode;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.SuggestParameters;
import com.esri.arcgisruntime.tasks.geocode.SuggestResult;
import com.esri.arcgisruntime.tasks.route.DirectionManeuver;

import android.view.ViewTreeObserver.OnGlobalLayoutListener;


/**
 * Implements the view that shows the map.
 */
/*public class MapFragment extends Fragment implements BasemapsDialogListener,
		RoutingDialogListener, OnCancelListener {*/
public class MapFragment extends Fragment  {

	public static final String TAG = MapFragment.class.getSimpleName();

	private static final String KEY_PORTAL_ITEM_ID = "KEY_PORTAL_ITEM_ID";

	private static final String KEY_BASEMAP_ITEM_ID = "KEY_BASEMAP_ITEM_ID";

	private static final String KEY_IS_LOCATION_TRACKING = "IsLocationTracking";

	private static final String COLUMN_NAME_ADDRESS = "address";

	private static final String COLUMN_NAME_X = "x";

	private static final String COLUMN_NAME_Y = "y";

	private static final int REQUEST_CODE_PROGRESS_DIALOG = 1;

	private static final String SEARCH_HINT = "Search";

	private static final String FIND_PLACE = "Find";

	private static final String SUGGEST_PLACE = "Suggest";

	private static FrameLayout.LayoutParams mlayoutParams;

	// Margins parameters for search view
	private static int TOP_MARGIN_SEARCH = 55;

	// The circle area specified by search_radius and input lat/lon serves
	// searching purpose.
	// It is also used to construct the extent which map zooms to after the
	// first
	// GPS fix is retrieved.
	private final static double SEARCH_RADIUS = 10;

	private String mPortalItemId;
	private String mBasemapPortalItemId;
	private PortalItem mPortalItem;
	private FrameLayout mMapContainer;
	public static MapView mMapView;
	private String mMapViewState;
	private SearchView mSearchview;

	// GPS location tracking
	private LocationDisplay mLocationDisplay;
	private boolean mIsLocationTracking;
	private Point mLocation = null;

	// Graphics layer to show geocode and reverse geocode results
	private GraphicsOverlay mLocationLayer;
	private Point mLocationLayerPoint;
	private String mLocationLayerPointString;

	// Graphics layer to show routes
	private GraphicsOverlay mRouteLayer;
	private List<DirectionManeuver> mRoutingDirections;

	// Spatial references used for projecting points
	private final SpatialReference mWm = SpatialReference.create(102100);
	private final SpatialReference mEgs = SpatialReference.create(4326);
	private MatrixCursor mSuggestionCursor;

	Compass mCompass;
	LayoutParams compassFrameParams;
	private MotionEvent mLongPressEvent;

	@SuppressWarnings("rawtypes")
	// - using this only to cancel pending tasks in a generic way
	private AsyncTask mPendingTask;
	private View mSearchBox;
	private LocatorTask mLocator;
	private View mSearchResult;
	private LayoutInflater mInflater;
	private String mStartLocation, mEndLocation;
	private SuggestParameters suggestParams;


	private final java.util.Map<String,Point> suggestMap = new TreeMap<>();
	private static ArrayList<SuggestResult> suggestionsList;

	private SpatialReference mapSpatialReference;
	private boolean suggestionClickFlag = false;
	private Point resultEndPoint;
	int width, height;

	LayoutParams gpsFrameParams;

	ImageButton navButton;
	DrawerLayout mDrawerLayout;
	ListView mDrawerList;

	public static MapFragment newInstance(String portalItemId,
										  String basemapPortalItemId) {
		MapFragment mapFragment = new MapFragment();

		Bundle args = new Bundle();
		args.putString(KEY_PORTAL_ITEM_ID, portalItemId);
		args.putString(KEY_BASEMAP_ITEM_ID, basemapPortalItemId);

		mapFragment.setArguments(args);
		return mapFragment;
	}

	public MapFragment() {
		// make MapFragment ctor private - use newInstance() instead
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);

		Bundle args = savedInstanceState != null ? savedInstanceState
				: getArguments();
		if (args != null) {
			mIsLocationTracking = args.getBoolean(KEY_IS_LOCATION_TRACKING);
			mPortalItemId = args.getString(KEY_PORTAL_ITEM_ID);
			mBasemapPortalItemId = args.getString(KEY_BASEMAP_ITEM_ID);
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// inflate MapView from layout

		mMapContainer = (FrameLayout) inflater.inflate(
				R.layout.map_fragment_layout, null);

		if (mPortalItemId != null) {
			// load the WebMap
			loadWebMapIntoMapView(mPortalItemId, mBasemapPortalItemId,
					AccountManager.getInstance().getPortal());
		} else {
			if (mBasemapPortalItemId != null) {
				// show a map with the basemap represented by
				// mBasemapPortalItemId
				loadWebMapIntoMapView(mBasemapPortalItemId, null,
						AccountManager.getInstance().getAGOLPortal());
			} else {
				// show the default map
				String defaultBaseMapURL = getString(R.string.default_basemap_url);
				Basemap basemap = new Basemap(defaultBaseMapURL);
				com.esri.arcgisruntime.mapping.Map map = new com.esri.arcgisruntime.mapping.Map(basemap);

				final MapView mapView =  (MapView) mMapContainer.findViewById(R.id.map);
				mapView.setMap(map);
				// Set the MapView to allow the user to rotate the map when as
				// part of a pinch gesture.
				//TODO: Modified for Quartz
				// mapView.setAllowRotationByPinch(true);

				setMapView(mapView);

				// TODO: Is this needed in Quartz?
				// mapView.zoomin();

				// TODO: Does this need to run on runOnUiThread?

				// Set up click listener on floating action button
				setUpFab(mapView);

				// Get an initial location on start up
				mLocationDisplay = mapView.getLocationDisplay();
				mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
					@Override
					public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {

						Point point = locationChangedEvent.getLocation().getPosition();
						if (point !=null){
							mLocationDisplay.removeLocationChangedListener(this);
							Log.i(TAG, "I have a location " + point.getX() + " , " + point.getY());
							showMyLocation(point);
						}


					}
				});
				mLocationDisplay.startAsync();

				//add graphics layer
				addGraphicLayers();
			}
		}

		return mMapContainer;
	}

	/**
	 * The floating action button toggles location tracking.  When location
	 * tracking is on, the compass is shown in the upper right of the map view.
	 * When location tracking is off, the compass is shown if the map is not oriented
	 * north (0 degrees).
	 * @param mapView
	 */
	private void setUpFab(final MapView mapView){
		final FloatingActionButton fab = (FloatingActionButton) mMapContainer.findViewById(R.id.fab);
		fab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mLocationDisplay = mapView.getLocationDisplay();

				// Toggle location tracking on or off
				if (mIsLocationTracking) {
					fab.setImageResource(R.drawable.ic_action_compass_mode);
					mLocationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS);
					mCompass.start();
					mCompass.setVisibility(View.VISIBLE);
					Log.i(TAG, "Location tracking on, compass should be visible");
					mIsLocationTracking = false;
				} else {
					fab.setImageResource(android.R.drawable.ic_menu_mylocation);
					mLocationDisplay.startAsync();
					mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
						@Override
						public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
							startLocationTracking(locationChangedEvent);
						}
					});
					if (mMapView.getMapRotation() != 0) {
						mCompass.setVisibility(View.VISIBLE);
						mCompass.setRotationAngle(mMapView.getMapRotation());
						Log.i(TAG, "No location tracking, map not pointed north, compass should be visible");
					} else {
						mCompass.setVisibility(View.GONE);
						Log.i(TAG, "No location tracking, map is pointed north, compass should not be visible");
					}
					mIsLocationTracking = true;
				}
			}
		});
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);

		// Inflate the menu items for use in the action bar
		inflater.inflate(R.menu.action, menu);

	}

	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

			default:
				return super.onOptionsItemSelected(item);
		}
	}


	@Override
	public void onPause() {
		super.onPause();

		// Pause the MapView and stop the LocationDisplayManager to save battery
		if (mMapView != null) {
			//TODO: Needed for Quartz?
			/*
			if (mIsLocationTracking) {
				mMapView.getLocationDisplay().
				mCompass.stop();
			}
			mMapViewState = mMapView.retainState();
			*/
			mMapView.pause();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		// Start the MapView and LocationDisplayManager running again
		if (mMapView != null) {
			// mCompass.start();
			mMapView.resume();
			// TODO: Need for Quartz?
			/*
			if (mMapViewState != null) {
				mMapView.restoreState(mMapViewState);
			}
			if (mIsLocationTracking) {
				mMapView.getLocationDisplayManager().start();
			}
			*/
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(KEY_IS_LOCATION_TRACKING, mIsLocationTracking);
		outState.putString(KEY_PORTAL_ITEM_ID, mPortalItemId);
		outState.putString(KEY_BASEMAP_ITEM_ID, mBasemapPortalItemId);
	}

	/**
	 * Loads a WebMap and creates a MapView from it which is set into the
	 * fragment's layout.
	 *
	 * @param portalItemId
	 *            The portal item id that represents the web map.
	 * @param basemapPortalItemId
	 *            The portal item id that represents the basemap.
	 * @throws Exception
	 *             if WebMap loading failed.
	 */
	private void loadWebMapIntoMapView(final String portalItemId,
									   final String basemapPortalItemId, final Portal portal) {

		TaskExecutor.getInstance().getThreadPool().submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {

				// load a WebMap instance from the portal item
				PortalItem portalItem = new PortalItem(portal, portalItemId);
				final com.esri.arcgisruntime.mapping.Map webmap = new com.esri.arcgisruntime.mapping.Map(portalItem);

				// load the WebMap that represents the basemap if one was
				// specified

				if (basemapPortalItemId != null
						&& !basemapPortalItemId.isEmpty()) {
					PortalItem webPortalItem = new PortalItem(portal, basemapPortalItemId);
					Basemap basemapWebMap = new Basemap(webPortalItem);
					webmap.setBasemap(basemapWebMap);
				}

				if (webmap != null) {
					// TODO: DO WE NEED to run this on RunOnUiThread IN QUARTZ?
					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							final MapView mapView =  (MapView) mMapContainer.findViewById(R.id.map);
							mapView.setMap(webmap);
							setMapView(mapView);
							setUpFab(mapView);
							addGraphicLayers();
						}
					});

				} else {
					throw new Exception("Failed to load web map.");
				}
				return null;
			}
		});
	}

	/**
	 * Takes a MapView that has already been instantiated to show a WebMap,
	 * completes its setup by setting various listeners and attributes, and sets
	 * it as the activity's content view.
	 *
	 * @param mapView
	 */
	private void setMapView(final MapView mapView) {

		mMapView = mapView;
		mMapView.setLogoVisible(true);
		mMapView.setWrapAroundMode(WrapAroundMode.ENABLE_WHEN_SUPPORTED);

		//TODO: Is this needed in Quartz?
		//mapView.setAllowRotationByPinch(true);

		// Creating an inflater
		mInflater = (LayoutInflater) getActivity().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);

		// Setting up the layout params for the searchview and searchresult
		// layout
		mlayoutParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP);
		int LEFT_MARGIN_SEARCH = 15;
		int RIGHT_MARGIN_SEARCH = 15;
		int BOTTOM_MARGIN_SEARCH = 0;
		mlayoutParams.setMargins(LEFT_MARGIN_SEARCH, TOP_MARGIN_SEARCH,
				RIGHT_MARGIN_SEARCH, BOTTOM_MARGIN_SEARCH);


		// Displaying the searchbox layout
		showSearchBoxLayout();


		// Set up location tracking
		mLocationDisplay = mapView.getLocationDisplay();
		mLocationDisplay.addLocationChangedListener(new LocationDisplay.LocationChangedListener() {
			@Override
			public void onLocationChanged(LocationDisplay.LocationChangedEvent locationChangedEvent) {
				startLocationTracking(locationChangedEvent);
			}
		});
		mLocationDisplay.startAsync();


		// TODO: Port to Quartz
		// Setup use of magnifier on a long press on the map
		mMapView.setMagnifierEnabled(true);
		mLongPressEvent = null;

		// Setup OnTouchListener to detect and act on long-press
		mMapView.setOnTouchListener(new MapView.OnTouchListener() {
			@Override
			public boolean onTwoFingerTap(MotionEvent motionEvent) {
				return false;
			}

			@Override
			public boolean onDoubleTouchDrag(MotionEvent motionEvent) {
				return false;
			}

			@Override
			public boolean onUp(MotionEvent motionEvent) {
				if (mLongPressEvent != null) {
					// This is the end of a long-press that will have displayed
					// the
					// magnifier.
					// Perform reverse-geocoding of the point that was pressed
					android.graphics.Point mapPoint = new android.graphics.Point((int) motionEvent.getX(), (int) motionEvent.getY());
					//Point mapPoint = mMapView.toMapPoint(to.getX(), to.getY());
					//TODO: Turn this back on when ReverseGeocoding is working
					//	ReverseGeocodingAsyncTask reverseGeocodeTask = new ReverseGeocodingAsyncTask();
					//	reverseGeocodeTask.execute(mapPoint);
					//	mPendingTask = reverseGeocodeTask;

					mLongPressEvent = null;
					// Remove any previous graphics
					resetGraphicsLayers();
				}
				//TODO: Anything else to do here?
				return true;
			}

			@Override
			public boolean onRotate(MotionEvent motionEvent, double v) {
				return false;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {

			}

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				return false;
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				// Set mLongPressEvent to indicate we are processing a
				// long-press
				mLongPressEvent = e;
				//TODO: Anything else to do here?
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				return false;
			}

			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				return false;
			}

			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				return false;
			}

			@Override
			public void onScaleEnd(ScaleGestureDetector detector) {

			}

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					// Start of a new gesture. Make sure mLongPressEvent is
					// cleared.
					mLongPressEvent = null;
				}
				//TODO: Anything else to do here?
				return true;
			}
		});

	}

	/**
	 * Adds the compass as per the height of the layout
	 *
	 * @param height
	 */
	private void addCompass(int height) {

		mMapContainer.removeView(mCompass);

		// Create the Compass custom view, and add it onto
		// the MapView.
		mCompass = new Compass(mMapView.getContext());
		mCompass.setAlpha(1f);
		mCompass.setRotationAngle(45);
		int HEIGHT = 240;
		int WIDTH = 240;
		compassFrameParams = new FrameLayout.LayoutParams(WIDTH, HEIGHT,
				Gravity.RIGHT);

		int TOP_MARGIN_COMPASS = TOP_MARGIN_SEARCH + height + 45;

		int LEFT_MARGIN_COMPASS = 0;
		int BOTTOM_MARGIN_COMPASS = 0;
		int RIGHT_MARGIN_COMPASS = 0;
		((MarginLayoutParams) compassFrameParams).setMargins(
				LEFT_MARGIN_COMPASS, TOP_MARGIN_COMPASS, RIGHT_MARGIN_COMPASS,
				BOTTOM_MARGIN_COMPASS);

		mCompass.setLayoutParams(compassFrameParams);

		mCompass.setVisibility(View.GONE);

		mCompass.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mCompass.setVisibility(View.GONE);
				mMapView.setRotation(0f);
				mMapView.setRotation(0f);
			}
		});

		// Add the compass on the map
		mMapContainer.addView(mCompass);

	}

	/**
	 *
	 * Displays the Dialog Fragment which allows users to route
	 */
	private void showRoutingDialogFragment() {

		suggestionClickFlag = false;
		// Show RoutingDialogFragment to get routing start and end points.
		// This calls back to onGetRoute() to do the routing.
		RoutingDialogFragment routingFrag = new RoutingDialogFragment();
		routingFrag.setRoutingDialogListener(new RoutingDialogListener() {
			@Override
			public boolean onGetRoute(String startPoint, String endPoint) {
				return false;
			}
		});
		Bundle arguments = new Bundle();
		if (mLocationLayerPoint != null) {
			arguments.putString(RoutingDialogFragment.ARG_END_POINT_DEFAULT,
					mLocationLayerPointString);
		}
		routingFrag.setArguments(arguments);
		routingFrag.show(getFragmentManager(), null);

	}

	/**
	 * Displays the Directions Dialog Fragment
	 */
	private void showDirectionsDialogFragment() {
		// Launch a DirectionsListFragment to display list of directions
		final DirectionsDialogFragment frag = new DirectionsDialogFragment();
		frag.setRoutingDirections(mRoutingDirections,
				new DirectionsDialogListener() {

					@Override
					public void onDirectionSelected(int position) {
						// User has selected a particular direction -
						// dismiss the dialog and
						// zoom to the selected direction
						frag.dismiss();
						DirectionManeuver direction = mRoutingDirections
								.get(position);

						// create a viewpoint from envelope
						Viewpoint vp = new Viewpoint(direction.getGeometry().getExtent());
						mMapView.setViewpoint(vp);
					}

				});
		getFragmentManager().beginTransaction().add(frag, null).commit();

	}

	/**
	 * Displays the search view layout
	 *
	 */
	private void showSearchBoxLayout() {

		// Inflating the layout from the xml file
		mSearchBox = mInflater.inflate(R.layout.searchview, null);
		// Inflate navigation drawer button on SearchView
		navButton = (ImageButton) mSearchBox.findViewById(R.id.btn_nav_menu);
		// Get the navigation drawer from Activity
		mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.maps_app_activity_drawer_layout);
		mDrawerList = (ListView) getActivity().findViewById(R.id.maps_app_activity_left_drawer);

		// Set click listener to open/close drawer
		navButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mDrawerLayout.isDrawerOpen(mDrawerList)){
					mDrawerLayout.closeDrawer(mDrawerList);
				}else{
					mDrawerLayout.openDrawer(mDrawerList);
				}

			}
		});

		// Setting the layout parameters to the layout
		mSearchBox.setLayoutParams(mlayoutParams);

		// Initializing the searchview and the image view
		mSearchview = (SearchView) mSearchBox
				.findViewById(R.id.searchView1);

		ImageView iv_route = (ImageView) mSearchBox
				.findViewById(R.id.imageView1);

		mSearchview.setIconifiedByDefault(false);
		mSearchview.setQueryHint(SEARCH_HINT);

		applySuggestionCursor();

//		navButton = (Button)mSearchBox.findViewById(R.id.navbutton);

		// Adding the layout to the map conatiner
		mMapContainer.addView(mSearchBox);

		// Setup the listener for the route onclick
		iv_route.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showRoutingDialogFragment();

			}
		});

		// Setup the listener when the search button is pressed on the keyboard
		mSearchview.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				onSearchButtonClicked(query);
				mSearchview.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				if(mLocator == null)
					return false;
				getSuggestions(newText);
				return true;
			}
		});

		// Add the compass after getting the height of the layout
		mSearchBox.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchBox.getHeight());
						mSearchBox.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

		mSearchview.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

			@Override
			public boolean onSuggestionSelect(int position) {
				return false;
			}

			@Override
			public boolean onSuggestionClick(int position) {
				// Obtain the content of the selected suggesting place via cursor
				MatrixCursor cursor = (MatrixCursor) mSearchview.getSuggestionsAdapter().getItem(position);
				int indexColumnSuggestion = cursor.getColumnIndex(COLUMN_NAME_ADDRESS);
				final String address = cursor.getString(indexColumnSuggestion);

				suggestionClickFlag = true;
				// Find the Location of the suggestion
				// TODO: Uncomment when Find Location task is working
				//new FindLocationTask(address).execute(address);

				cursor.close();

				return true;
			}
		});

	}

	/**
	 * Initialize Suggestion Cursor
	 */
	private void initSuggestionCursor() {
		String[] cols = new String[]{BaseColumns._ID, COLUMN_NAME_ADDRESS, COLUMN_NAME_X, COLUMN_NAME_Y};
		mSuggestionCursor = new MatrixCursor(cols);
	}

	/**
	 * Set the suggestion cursor to an Adapter then set it to the search view
	 */
	private void applySuggestionCursor() {
		String[] cols = new String[]{COLUMN_NAME_ADDRESS};
		int[] to = new int[]{R.id.suggestion_item_address};

		SimpleCursorAdapter mSuggestionAdapter = new SimpleCursorAdapter(mMapView.getContext(), R.layout.search_suggestion_item, mSuggestionCursor, cols, to, 0);
		mSearchview.setSuggestionsAdapter(mSuggestionAdapter);
		mSuggestionAdapter.notifyDataSetChanged();
	}


	/**
	 * Provide a character by character suggestions for the search string
	 *
	 * @param query String typed so far by the user to fetch the suggestions
	 */
	private void getSuggestions(String query) {
		//TODO: Uncomment this when suggestion stuff is figured out
		/*
		final CallbackListener<List<LocatorSuggestionResult>> suggestCallback = new CallbackListener<List<LocatorSuggestionResult>>() {
			@Override
			public void onCallback(List<LocatorSuggestionResult> locatorSuggestionResults) {
				final List<LocatorSuggestionResult> locSuggestionResults = locatorSuggestionResults;
				if (locatorSuggestionResults == null)
					return;
				suggestionsList = new ArrayList<>();
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						int key = 0;
						if (locSuggestionResults.size() > 0) {
							// Add suggestion list to a cursor
							initSuggestionCursor();
							for (final LocatorSuggestionResult result : locSuggestionResults) {

								// add the locatorSuggestionResult to the ArrayList
								suggestionsList.add(result);

								// Add the suggestion results to the cursor
								mSuggestionCursor.addRow(new Object[]{key++, result.getText(), "0", "0"});
							}

							applySuggestionCursor();
						}
					}

				});

			}

			@Override
			public void onError(Throwable throwable) {

				//Log the error
				Log.e(MapFragment.class.getSimpleName(), "No Results found!!");
				Log.e(MapFragment.class.getSimpleName(), throwable.getMessage());
			}
		};

		try {
			// Initialize the locatorSugestion parameters
			locatorParams(SUGGEST_PLACE,query);

			mLocator.suggest(suggestParams, suggestCallback);
		}
		catch (Exception e) {
			Log.e(MapFragment.class.getSimpleName(),"No Results found");
			Log.e(MapFragment.class.getSimpleName(),e.getMessage());
		}
		*/
	}

	/**
	 * Initialize LocatorSuggestionParameters or LocatorFindParameters
	 *
	 * @param TYPE A String determining thr type of parameters to be initialized
	 * @param query The string for which the locator parameters are to be initialized
	 */
	/*
	private void locatorParams(String TYPE, String query) {
		if(TYPE.contentEquals(SUGGEST_PLACE)) {
			// Create suggestion parameters
			suggestParams = new LocatorSuggestionParameters(query);

			// Use the centre of the current map extent as the location
			suggestParams.setLocation(mMapView.getCenter(),
					mMapView.getSpatialReference());

			// Calculate distance for search operation
			Envelope mapExtent = new Envelope();
			mMapView.getExtent().queryEnvelope(mapExtent);

			// assume map is in meters, other units wont work, double
			// current envelope
			double distance = (mapExtent.getWidth() > 0) ? mapExtent
					.getWidth() * 2 : 10000;
			suggestParams.setDistance(distance);



		}

		if (TYPE.contentEquals(FIND_PLACE)) {
			// Create find parameters
			LocatorFindParameters findParams = new LocatorFindParameters(query);

			// Use the centre of the current map extent as the location
			findParams.setLocation(mMapView.getCenter(),
			mMapView.getSpatialReference());

			// Calculate distance for search operation
			Envelope mapExtent = new Envelope();
			mMapView.getExtent().queryEnvelope(mapExtent);

			// assume map is in meters, double current envelope
			double distance = (mapExtent.getWidth() > 0) ? mapExtent
					.getWidth() * 2 : 10000;
			findParams.setDistance(distance);

			findParams.setOutSR(mMapView.getSpatialReference());
		}
	}
*/
	//Fetch the Location from the suggestMap and display it
/*	private class FindLocationTask extends AsyncTask<String,Void,Point> {
		private Point resultPoint = null;
		private String resultAddress;
		private static final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";

		private ProgressDialogFragment mProgressDialog;

		public FindLocationTask(String address) {
			resultAddress = address;
		}

		@Override
		protected Point doInBackground(String... params) {

			// get the Location for the suggestion from the ArrayList
			for(LocatorSuggestionResult result: suggestionsList) {
				if(resultAddress.matches(result.getText())) {
					try {
						resultPoint = ((mLocator.find(result, 2, null, mapSpatialReference)).get(0)).getLocation();
					} catch (Exception e) {
						Log.e(TAG,"Error in fetching the Location");
						Log.e(TAG,e.getMessage());
					}
				}
			}

			resultEndPoint = resultPoint;

			return resultPoint;
		}

		@Override
		protected void onPreExecute() {
			// Display progress dialog on UI thread
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.address_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_LOCATOR_PROGRESS_DIALOG);
		}

		@Override
		protected void onPostExecute(Point resultPoint) {
			// Dismiss progress dialog
			mProgressDialog.dismiss();
			if (resultPoint == null)
				return;

			// Display the result
			displaySearchResult(resultPoint,resultAddress);
			hideKeyboard();
		}

	}
*/
	protected void hideKeyboard() {

		// Hide soft keyboard
		mSearchview.clearFocus();
		InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
	}

	private void displaySearchResult(Point resultPoint, String address) {

		// create marker symbol to represent location
		Bitmap icon = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.pin_circle_red);
		BitmapDrawable drawable = new BitmapDrawable(getActivity().getResources(), icon);
		PictureMarkerSymbol resultSymbol = new PictureMarkerSymbol(drawable);
		// create graphic object for resulting location
		Graphic resultLocGraphic = new Graphic(resultPoint,
                    resultSymbol);
		// add graphic to location layer
		mLocationLayer.getGraphics().add(resultLocGraphic);

		mLocationLayerPoint = resultPoint;

		mLocationLayerPointString = address;

		// Zoom map to geocode result location

		//mMapView.zoomToResolution(resultPoint, 2);
		showSearchResultLayout(address);
	}


        /**
         * Clears all graphics out of the location layer and the route layer.
         */
	void resetGraphicsLayers() {
		//TODO: Is clearSelection the same as GraphicsLayer removeAll
		mLocationLayer.getGraphics().clear();
		mRouteLayer.getGraphics().clear();
		mLocationLayerPoint = null;
		mLocationLayerPointString = null;
		mRoutingDirections = null;
	}

	/**
	 * Adds location layer and the route layer to the MapView.
	 */
	void addGraphicLayers() {
		// Add location layer
		if (mLocationLayer == null) {
			mLocationLayer = new GraphicsOverlay();
		}

		mMapView.getGraphicsOverlays().add(mLocationLayer);

		// Add the route graphic layer
		if (mRouteLayer == null) {
			mRouteLayer =  new GraphicsOverlay();
		}
		mMapView.getGraphicsOverlays().add(mRouteLayer);
	}

	/**
	 * Starts tracking GPS location.
	 */
	void startLocationTracking(LocationDisplay.LocationChangedEvent locationChangedEvent) {

		mCompass.start();
		// Enabling the line below causes the map to not zoom in on my location
		//locDispMgr.setAutoPanMode(LocationDisplay.AutoPanMode.DEFAULT);
		//TODO: How to in Quartz?
		//locDispMgr.setAllowNetworkLocation(true);


		boolean locationChanged = false;
		Point wgsPoint = locationChangedEvent.getLocation().getPosition();

		if (!locationChanged) {
			locationChanged = true;
			showMyLocation(wgsPoint);
		}
		mIsLocationTracking = true;
	}

	private void showMyLocation(Point wgsPoint){
		if (mMapView.getSpatialReference() != null){
			mLocation = (Point) GeometryEngine.project(wgsPoint,
					mMapView.getSpatialReference());
			LinearUnit mapUnit = (LinearUnit) mMapView.getSpatialReference().getUnit();
			LinearUnit mile = new LinearUnit(LinearUnitId.MILES);

			double zoomWidth = mile.convertTo(mapUnit,SEARCH_RADIUS);
			double width = zoomWidth/10 ;
			double height = zoomWidth/10;

			Point envPoint = new Point(mLocation.getX()-width, mLocation.getY()-height, mMapView.getSpatialReference());
			Point envPointB = new Point(mLocation.getX()+width, mLocation.getY()+height, mMapView.getSpatialReference());

			Envelope zoomExtent = new Envelope(envPoint, envPointB);
			mMapView.setViewpointGeometryAsync(zoomExtent);
		}

	}

	//@Override
	/*public void onBasemapChanged(String basemapPortalItemId) {
		((MapsAppActivity) getActivity()).showMap(mPortalItemId,
				basemapPortalItemId);
	}

	/**
	 * Called from search_layout.xml when user presses Search button.
	 *
	 * @param address
	 */
	public void onSearchButtonClicked(final String address) {

		// Hide virtual keyboard
		hideKeyboard();

		// Remove any previous graphics and routes
		resetGraphicsLayers();
		// TODO: Un comment once Locator task is working
		executeLocatorTask(address);
	}

	/**
	 * Set up the search parameters and execute the Locator task.
	 *
	 * @param address
	 */
	private void executeLocatorTask(final String address) {
		ArcGISRuntimeEnvironment.License.setLicense(getString(R.string.license));
		// Create Locator parameters from single line address string
		final GeocodeParameters geoParameters = new GeocodeParameters();
		geoParameters.setMaxResults(2);

		// Use the centre of the current map extent as the find location point
		if (mLocation != null){
			geoParameters.setPreferredSearchLocation(mLocation);
		}

		// Set address spatial reference to match map
		SpatialReference sR = mMapView.getSpatialReference();
		geoParameters.setOutputSpatialReference(sR);

		Polygon polygon = mMapView.getVisibleArea();
		Envelope mapExtent = polygon.getExtent();
		// Calculate distance for find operation

		// assume map is in metres, other units wont work, double current
		// envelope
		double width = (mapExtent.getWidth() > 0) ? mapExtent
				.getWidth() * 2 : 10000;
		double height = (mapExtent.getHeight() > 0) ? mapExtent
				.getHeight() * 2 : 10000;
		double xMax = mapExtent.getXMax() + width;
		double xMin = mapExtent.getXMin() - width;
		double yMax = mapExtent.getYMax() + height;
		double yMin = mapExtent.getYMin() - height;

		geoParameters.setSearchArea(new Envelope(new Point(xMax,yMax, sR), new Point(xMin, yMin, sR)));
		
		// Execute async task to find the address
		final LocatorTask locatorTask = new LocatorTask("http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
		locatorTask.addDoneLoadingListener(new Runnable() {
			@Override
			public void run() {
				if (locatorTask.getLoadStatus() == LoadStatus.LOADED){
					// Call geocodeAsync passing in an address
					final ListenableFuture<List<GeocodeResult>> geocodeFuture = locatorTask.geocodeAsync(address, geoParameters);
					geocodeFuture.addDoneListener(new Runnable() {
						@Override
						public void run() {
							try {
								// Get the results of the async operation
								List<GeocodeResult> geocodeResults = geocodeFuture.get();

								if (geocodeResults.size() > 0) {
									// Use the first result - for example display on the map
									GeocodeResult topResult = geocodeResults.get(0);
									displaySearchResult(topResult.getDisplayLocation(),address);

									Log.i(TAG, topResult.getDisplayLocation().getX() + " " + topResult.getDisplayLocation().getY());


								}
							} catch (InterruptedException e) {
								// Deal with exception...
								e.printStackTrace();
							} catch (ExecutionException e) {
								// Deal with exception...
								e.printStackTrace();
							}
						}
					});
				}else{
					Log.i(TAG, "Locator task error");
				}
			}
		});
		locatorTask.loadAsync();
	}

	/**
	 * Called by RoutingDialogFragment when user presses Get Route button.
	 *
	 * @param startPoint
	 *            String entered by user to define start point.
	 * @param endPoint
	 *            String entered by user to define end point.
	 * @return true if routing task executed, false if parameters rejected. If
	 *         this method rejects the parameters it must display an explanatory
	 *         Toast to the user before returning.
	 */
	//@Override
	public boolean onGetRoute(String startPoint, String endPoint) {
		// Check if we need a location fix
		if (startPoint.equals(getString(R.string.my_location))
				&& mLocation == null) {
			Toast.makeText(getActivity(),
					getString(R.string.need_location_fix), Toast.LENGTH_LONG)
					.show();
			return false;
		}
		// Remove any previous graphics and routes
		resetGraphicsLayers();

		// Do the routing
		// TODO: Uncomment when routing task is working
		//executeRoutingTask(startPoint, endPoint);
		return true;
	}

	/**
	 * Set up Route Parameters to execute RouteTask
	 *
	 * @param start
	 * @param end
	 */
/*
	@SuppressWarnings("unchecked")
	private void executeRoutingTask(String start, String end) {
		resetGraphicsLayers();
		// Create a list of start end point params
		LocatorFindParameters routeStartParams = new LocatorFindParameters(
				start);
		LocatorFindParameters routeEndParams = new LocatorFindParameters(end);
		List<LocatorFindParameters> routeParams = new ArrayList<>();

		// Add params to list
		routeParams.add(routeStartParams);
		routeParams.add(routeEndParams);

		// Execute async task to do the routing
		RouteAsyncTask routeTask = new RouteAsyncTask();
		routeTask.execute(routeParams);
		mPendingTask = routeTask;
	}
*/
	//@Override
	public void onCancel(DialogInterface dialog) {
		// a pending task needs to be canceled
		if (mPendingTask != null) {
			mPendingTask.cancel(true);
		}
	}

	/**
	 * Shows the search result in the layout after successful geocoding and
	 * reverse geocoding
	 *
	 */

	private void showSearchResultLayout(String address) {
		// Remove the layouts
		mMapContainer.removeView(mSearchBox);
		mMapContainer.removeView(mSearchResult);

		// Inflate the new layout from the xml file
		mSearchResult = mInflater.inflate(R.layout.search_result, null);

		// Set layout parameters
		mSearchResult.setLayoutParams(mlayoutParams);

		// Initialize the textview and set its text
		TextView tv = (TextView) mSearchResult.findViewById(R.id.textView1);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setText(address);

		// Adding the search result layout to the map container
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult
				.findViewById(R.id.imageView3);
		iv_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Remove the search result view
				mMapContainer.removeView(mSearchResult);

				suggestionClickFlag = false;
				// Add the search box view
				showSearchBoxLayout();

				// Remove all graphics from the map
				resetGraphicsLayers();

			}
		});

		// Set up the listener for the "Get Directions" icon
		ImageView iv_route = (ImageView) mSearchResult
				.findViewById(R.id.imageView2);
		iv_route.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onGetRoute(getString(R.string.my_location),
						mLocationLayerPointString);
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchResult.getHeight());
						mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

	}

	/**
	 * Shows the Routing result layout after successful routing
	 *
	 * @param time
	 * @param distance
	 *
	 */

	private void showRoutingResultLayout(double distance, double time) {

		// Remove the layours
		mMapContainer.removeView(mSearchResult);
		mMapContainer.removeView(mSearchBox);

		// Inflate the new layout from the xml file
		mSearchResult = mInflater.inflate(R.layout.routing_result, null);

		mSearchResult.setLayoutParams(mlayoutParams);

		// Shorten the start and end location by finding the first comma if
		// present
		int index_from = mStartLocation.indexOf(",");
		int index_to = mEndLocation.indexOf(",");
		if (index_from != -1)
			mStartLocation = mStartLocation.substring(0, index_from);
		if (index_to != -1)
			mEndLocation = mEndLocation.substring(0, index_to);

		// Initialize the textvieww and display the text
		TextView tv_from = (TextView) mSearchResult.findViewById(R.id.tv_from);
		tv_from.setTypeface(null, Typeface.BOLD);
		tv_from.setText(" " + mStartLocation);

		TextView tv_to = (TextView) mSearchResult.findViewById(R.id.tv_to);
		tv_to.setTypeface(null, Typeface.BOLD);
		tv_to.setText(" " + mEndLocation);

		// Rounding off the values
		distance = Math.round(distance * 10.0) / 10.0;
		time = Math.round(time * 10.0) / 10.0;

		TextView tv_time = (TextView) mSearchResult.findViewById(R.id.tv_time);
		tv_time.setTypeface(null, Typeface.BOLD);
		tv_time.setText(time + " mins");

		TextView tv_dist = (TextView) mSearchResult.findViewById(R.id.tv_dist);
		tv_dist.setTypeface(null, Typeface.BOLD);
		tv_dist.setText(" (" + distance + " miles)");

		// Adding the layout
		mMapContainer.addView(mSearchResult);

		// Setup the listener for the "Cancel" icon
		ImageView iv_cancel = (ImageView) mSearchResult
				.findViewById(R.id.imageView3);
		iv_cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Remove the search result view
				mMapContainer.removeView(mSearchResult);
				// Add the default search box view
				showSearchBoxLayout();
				// Remove all graphics from the map
				resetGraphicsLayers();

			}
		});

		// Set up the listener for the "Show Directions" icon
		ImageView iv_directions = (ImageView) mSearchResult
				.findViewById(R.id.imageView2);
		iv_directions.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDirectionsDialogFragment();
			}
		});

		// Add the compass after getting the height of the layout
		mSearchResult.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						addCompass(mSearchResult.getHeight());
						mSearchResult.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					}

				});

	}

	/*
	 * This class provides an AsyncTask that performs a geolocation request on a
	 * background thread and displays the first result on the map on the UI
	 * thread.
	 */
/*	private class LocatorAsyncTask extends
			AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
		private static final String TAG_LOCATOR_PROGRESS_DIALOG = "TAG_LOCATOR_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		public LocatorAsyncTask() {
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.address_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_LOCATOR_PROGRESS_DIALOG);
		}

		@Override
		protected List<LocatorGeocodeResult> doInBackground(
				LocatorFindParameters... params) {
			// Perform routing request on background thread
			mException = null;
			List<LocatorGeocodeResult> results = null;

			// Create locator using default online geocoding service and tell it
			// to
			// find the given address
			Locator locator = Locator.createOnlineLocator();
			try {
				results = locator.find(params[0]);
			} catch (Exception e) {
				mException = e;
			}
			return results;
		}

		@Override
		protected void onPostExecute(List<LocatorGeocodeResult> result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "LocatorSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.addressSearchFailed),
						Toast.LENGTH_LONG).show();
				return;
			}

			if (result.size() == 0) {
				Toast.makeText(getActivity(),
						getString(R.string.noResultsFound), Toast.LENGTH_LONG)
						.show();
			} else {
				// Use first result in the list
				LocatorGeocodeResult geocodeResult = result.get(0);

				// get return geometry from geocode result
				Point resultPoint = geocodeResult.getLocation();

				// Get the address
				String address = geocodeResult.getAddress();

				// Display the result on the map
				displaySearchResult(resultPoint,address);

			}
		}

	}

	/**
	 * This class provides an AsyncTask that performs a routing request on a
	 * background thread and displays the resultant route on the map on the UI
	 * thread.
	 */
/*	private class RouteAsyncTask extends
			AsyncTask<List<LocatorFindParameters>, Void, RouteResult> {
		private static final String TAG_ROUTE_SEARCH_PROGRESS_DIALOG = "TAG_ROUTE_SEARCH_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		public RouteAsyncTask() {
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.route_search));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_ROUTE_SEARCH_PROGRESS_DIALOG);
		}

		@Override
		protected RouteResult doInBackground(
				List<LocatorFindParameters>... params) {
			// Perform routing request on background thread
			mException = null;

			// Define route objects
			List<LocatorGeocodeResult> geocodeStartResult;
			List<LocatorGeocodeResult> geocodeEndResult;
			Point startPoint;
			Point endPoint;

			// Create a new locator to geocode start/end points;
			// by default uses ArcGIS online world geocoding service
			Locator locator = Locator.createOnlineLocator();

			try {
				// Geocode start position, or use My Location (from GPS)
				LocatorFindParameters startParam = params[0].get(0);
				if (startParam.getText()
						.equals(getString(R.string.my_location))) {
					mStartLocation = getString(R.string.my_location);
					startPoint = (Point) GeometryEngine.project(mLocation, mWm,
							mEgs);
				} else {
					geocodeStartResult = locator.find(startParam);
					startPoint = geocodeStartResult.get(0).getLocation();
					mStartLocation = geocodeStartResult.get(0).getAddress();

					if (isCancelled()) {
						return null;
					}
				}

				// Geocode the destination
				LocatorFindParameters endParam = params[0].get(1);
				if (endParam.getText().equals(getString(R.string.my_location))) {
					mEndLocation = getString(R.string.my_location);
					endPoint = (Point) GeometryEngine.project(mLocation, mWm,
							mEgs);
				} else {
					geocodeEndResult = locator.find(endParam);
					if (suggestionClickFlag) {

						endPoint = (Point) GeometryEngine.project(resultEndPoint, mWm, mEgs);
						mEndLocation = endParam.getText();
					} else {
						endPoint = geocodeEndResult.get(0).getLocation();
						mEndLocation = geocodeEndResult.get(0).getAddress();
					}
				}

			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}

			// Create a new routing task pointing to an ArcGIS Network Analysis
			// Service
			RouteTask routeTask;
			RouteParameters routeParams;
			try {
				routeTask = RouteTask.createOnlineRouteTask(
						getString(R.string.routingservice_url), null);
				// Retrieve default routing parameters
				routeParams = routeTask.retrieveDefaultRouteTaskParameters();
			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}

			// Customize the route parameters
			NAFeaturesAsFeature routeFAF = new NAFeaturesAsFeature();
			StopGraphic sgStart = new StopGraphic(startPoint);
			StopGraphic sgEnd = new StopGraphic(endPoint);
			routeFAF.setFeatures(new Graphic[]{sgStart, sgEnd});
			routeFAF.setCompressedRequest(true);
			routeParams.setStops(routeFAF);
			routeParams.setOutSpatialReference(mMapView.getSpatialReference());

			// Solve the route
			RouteResult routeResult;
			try {
				routeResult = routeTask.solve(routeParams);
			} catch (Exception e) {
				mException = e;
				return null;
			}
			if (isCancelled()) {
				return null;
			}
			return routeResult;
		}

		@Override
		protected void onPostExecute(RouteResult result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "RouteSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.routingFailed), Toast.LENGTH_LONG)
						.show();
				return;
			}

			// Get first item in list of routes provided by server
			Route route;
			try {
				route = result.getRoutes().get(0);
				if( route.getTotalMiles() == 0.0 || route.getTotalKilometers() == 0.0 ) {
					throw new Exception("Can not find the Route");
				}
			} catch (Exception e) {
				Toast.makeText(getActivity(), "We are sorry, we couldn't find the route. Please make " +
								"sure the Source and Destination are different or are connected by road",
						Toast.LENGTH_LONG).show();
				Log.e(TAG,e.getMessage());
				return;
			}


			// Create polyline graphic of the full route
			SimpleLineSymbol lineSymbol = new SimpleLineSymbol(Color.RED, 2,
					STYLE.SOLID);
			Graphic routeGraphic = new Graphic(route.getRouteGraphic()
					.getGeometry(), lineSymbol);

			// Create point graphic to mark start of route
			Point startPoint = ((Polyline) routeGraphic.getGeometry())
					.getPoint(0);
			Graphic startGraphic = createMarkerGraphic(startPoint, false);

			// Create point graphic to mark end of route
			int endPointIndex = ((Polyline) routeGraphic.getGeometry())
					.getPointCount() - 1;
			Point endPoint = ((Polyline) routeGraphic.getGeometry())
					.getPoint(endPointIndex);
			Graphic endGraphic = createMarkerGraphic(endPoint, true);

			// Add these graphics to route layer
			mRouteLayer.addGraphics(new Graphic[]{routeGraphic, startGraphic,
					endGraphic});

			// Zoom to the extent of the entire route with a padding
			mMapView.setExtent(route.getEnvelope(), 100);

			// Save routing directions so user can display them later
			mRoutingDirections = route.getRoutingDirections();

			// Show Routing Result Layout
			showRoutingResultLayout(route.getTotalMiles(),
					route.getTotalMinutes());

		}

		Graphic createMarkerGraphic(Point point, boolean endPoint) {
			Drawable marker = getResources().getDrawable(
					endPoint ? R.drawable.pin_circle_blue
							: R.drawable.pin_circle_red);
			PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(
					mMapView.getContext(), marker);
			// NOTE: marker's bounds not set till marker is used to create
			// destinationSymbol
			float offsetY = convertPixelsToDp(getActivity(),
					marker != null ? marker.getBounds().bottom : 0);
			destinationSymbol.setOffsetY(offsetY);
			return new Graphic(point, destinationSymbol);
		}
	}

	/**
	 * This class provides an AsyncTask that performs a reverse geocoding
	 * request on a background thread and displays the resultant point on the
	 * map on the UI thread.
	 */
/*	public class ReverseGeocodingAsyncTask extends
			AsyncTask<Point, Void, LocatorReverseGeocodeResult> {
		private static final String TAG_REVERSE_GEOCODING_PROGRESS_DIALOG = "TAG_REVERSE_GEOCODING_PROGRESS_DIALOG";

		private Exception mException;

		private ProgressDialogFragment mProgressDialog;

		private Point mPoint;

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialogFragment.newInstance(getActivity()
					.getString(R.string.reverse_geocoding));
			// set the target fragment to receive cancel notification
			mProgressDialog.setTargetFragment(MapFragment.this,
					REQUEST_CODE_PROGRESS_DIALOG);
			mProgressDialog.show(getActivity().getFragmentManager(),
					TAG_REVERSE_GEOCODING_PROGRESS_DIALOG);
		}

		@Override
		protected LocatorReverseGeocodeResult doInBackground(Point... params) {
			// Perform reverse geocoding request on background thread
			mException = null;
			LocatorReverseGeocodeResult result = null;
			mPoint = params[0];

			// Create locator using default online geocoding service and tell it
			// to
			// find the given point
			Locator locator = Locator.createOnlineLocator();
			try {
				// Our input and output spatial reference will be the same as
				// the map
				SpatialReference mapRef = mMapView.getSpatialReference();
				result = locator.reverseGeocode(mPoint, 100.0, mapRef, mapRef);
				mLocationLayerPoint = mPoint;
			} catch (Exception e) {
				mException = e;
			}
			// return the resulting point(s)
			return result;
		}

		@Override
		protected void onPostExecute(LocatorReverseGeocodeResult result) {
			// Display results on UI thread
			mProgressDialog.dismiss();
			if (mException != null) {
				Log.w(TAG, "LocatorSyncTask failed with:");
				mException.printStackTrace();
				Toast.makeText(getActivity(),
						getString(R.string.addressSearchFailed),
						Toast.LENGTH_LONG).show();
				return;
			}

			// Construct a nicely formatted address from the results
			StringBuilder address = new StringBuilder();
			if (result != null && result.getAddressFields() != null) {
				Map<String, String> addressFields = result.getAddressFields();
				address.append(String.format("%s\n%s, %s %s",
						addressFields.get("Address"),
						addressFields.get("City"), addressFields.get("Region"),
						addressFields.get("Postal")));

				// Draw marker on map.
				// create marker symbol to represent location
				Drawable drawable = getActivity().getResources().getDrawable(
						R.drawable.pin_circle_red);
				PictureMarkerSymbol symbol = new PictureMarkerSymbol(
						getActivity(), drawable);
				mLocationLayer.addGraphic(new Graphic(mPoint, symbol));

				// Address string is saved for use in routing
				mLocationLayerPointString = address.toString();
				// center the map to result location
				mMapView.centerAt(mPoint, true);

				// Show the result on the search result layout
				showSearchResultLayout(address.toString());
			}
		}
	}

	/**
	 * Converts device specific pixels to density independent pixels.
	 *
	 * @param context
	 * @param px
	 *            number of device specific pixels
	 * @return number of density independent pixels
	 */
	private float convertPixelsToDp(Context context, float px) {
		DisplayMetrics metrics = context.getResources().getDisplayMetrics();
		return px / (metrics.densityDpi / 160f);
	}

	public void setPortalItem(PortalItem item) {
		mPortalItem = item;
	}
}

