package com.vocifery.daytripper.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.beyondar.android.fragment.BeyondarFragmentSupport;
import com.beyondar.android.opengl.colision.MeshCollider;
import com.beyondar.android.opengl.colision.SquareMeshCollider;
import com.beyondar.android.plugin.radar.RadarPointPlugin;
import com.beyondar.android.plugin.radar.RadarView;
import com.beyondar.android.plugin.radar.RadarWorldPlugin;
import com.beyondar.android.util.ImageUtils;
import com.beyondar.android.util.math.geom.Point3;
import com.beyondar.android.view.BeyondarGLSurfaceView;
import com.beyondar.android.view.OnClickBeyondarObjectListener;
import com.beyondar.android.view.OnTouchBeyondarViewListener;
import com.beyondar.android.world.BeyondarObject;
import com.beyondar.android.world.BeyondarObjectList;
import com.beyondar.android.world.GeoObject;
import com.beyondar.android.world.World;
import com.vocifery.daytripper.Daytripper;
import com.vocifery.daytripper.R;
import com.vocifery.daytripper.util.ImageLoader;
import com.vocifery.daytripper.util.MetricUtils;
import com.vocifery.daytripper.vocifery.model.Searchable;

public class HeadsUpActivity extends FragmentActivity implements
		OnSeekBarChangeListener, OnTouchBeyondarViewListener,
		OnClickBeyondarObjectListener {

	public final static String WORLD_LAT = "com.vocifery.daytripper.ui.WORLD_LAT";
	public final static String WORLD_LON = "com.vocifery.daytripper.ui.WORLD_LON";
	public final static String WORLD_POINTS = "com.vocifery.daytripper.ui.WORLD_POINTS";

	private BeyondarFragmentSupport beyondar;
	private World world;
	private RadarView radarView;
	private RadarWorldPlugin radarPlugin;
	private List<BeyondarImageWorker> imageWorkerList;
	private SeekBar mSeekBarPullCloserDistance, mSeekBarPushAwayDistance,
			mSeekBarMaxDistanceToRender, mSeekBarDistanceFactor;
	private TextView mMaxFarText, mMinFarText, mArViewDistanceText, mZfarText;

	private final static String TAG = "HeadsUpActivity";
	private final static String TMP_IMAGE_PREFIX = "viewImage_";

	private final static int INITIAL_DISTANCE_TO_RENDER = 100;
	private final static int MAX_DISTANCE_TO_RENDER = 20000;

	private final static int INITIAL_PULL_CLOSER_DISTANCE = 115;
	private final static int MAX_PULL_CLOSER_DISTANCE = 1000;

	private final static int INITIAL_PUSH_AWAY_DISTANCE = 115;
	private final static int MAX_PUSH_AWAY_DISTANCE = 1000;

	private final static int INITIAL_DISTANCE_FACTOR = 1;
	private final static int MAX_DISTANCE_FACTOR = 10;

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.headsup_view);

		try {
			cleanTempFolder();
			Intent intent = getIntent();
			ArrayList<Searchable> worldPoints = (ArrayList<Searchable>) intent
					.getSerializableExtra(WORLD_POINTS);
			beyondar = (BeyondarFragmentSupport) getSupportFragmentManager()
					.findFragmentById(R.id.beyondarFragment);

			radarView = (RadarView) findViewById(R.id.radarView);
			// Create the Radar plugin
			radarPlugin = new RadarWorldPlugin(this);
			// set the radar view in to our radar plugin
			radarPlugin.setRadarView(radarView);
			// Set how far (in meters) we want to display in the view
			radarPlugin.setMaxDistance(MAX_DISTANCE_TO_RENDER);
			// We can customize the color of the items
			radarPlugin.setListColor(World.LIST_TYPE_DEFAULT, getResources()
					.getColor(R.color.primary));
			// and also the size
			radarPlugin.setListDotRadius(World.LIST_TYPE_DEFAULT, 3);

			mMaxFarText = (TextView) findViewById(R.id.textBarMax);
			mMinFarText = (TextView) findViewById(R.id.textBarMin);
			mArViewDistanceText = (TextView) findViewById(R.id.textBarArViewDistance);
			mZfarText = (TextView) findViewById(R.id.textBarZFar);

			mSeekBarPullCloserDistance = (SeekBar) findViewById(R.id.seekBarMax);
			mSeekBarPushAwayDistance = (SeekBar) findViewById(R.id.seekBarMin);
			mSeekBarMaxDistanceToRender = (SeekBar) findViewById(R.id.seekBarArViewDistance);
			mSeekBarDistanceFactor = (SeekBar) findViewById(R.id.seekBarZFar);

			mSeekBarPullCloserDistance.setOnSeekBarChangeListener(this);
			mSeekBarPushAwayDistance.setOnSeekBarChangeListener(this);
			mSeekBarMaxDistanceToRender.setOnSeekBarChangeListener(this);
			mSeekBarDistanceFactor.setOnSeekBarChangeListener(this);

			mSeekBarPullCloserDistance.setMax(MAX_PULL_CLOSER_DISTANCE);
			mSeekBarPullCloserDistance
					.setProgress(INITIAL_PULL_CLOSER_DISTANCE);

			mSeekBarPushAwayDistance.setMax(MAX_PUSH_AWAY_DISTANCE);
			mSeekBarPushAwayDistance.setProgress(INITIAL_PUSH_AWAY_DISTANCE);

			mSeekBarMaxDistanceToRender.setMax(MAX_DISTANCE_TO_RENDER);
			mSeekBarMaxDistanceToRender.setProgress(INITIAL_DISTANCE_TO_RENDER);

			mSeekBarDistanceFactor.setMax(MAX_DISTANCE_FACTOR);
			mSeekBarDistanceFactor.setProgress(INITIAL_DISTANCE_FACTOR);

			if (worldPoints != null) {
				Double worldLat = intent.getDoubleExtra(WORLD_LAT, 0d);
				Double worldLon = intent.getDoubleExtra(WORLD_LON, 0d);
				initRealWorld(worldLat, worldLon, worldPoints);
			}

			beyondar.setWorld(world);
			world.addPlugin(radarPlugin);
			beyondar.showFPS(false);
			replaceImagesByStaticViews(world);
			lockOrientation(true);
		} catch (Exception e) {
			Log.e(TAG, "World initialization error - " + e.getMessage());
			finish();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		if (radarPlugin != null) {
			if (seekBar == mSeekBarMaxDistanceToRender) {
				radarPlugin.setMaxDistance(progress);
			}
		}

		if (seekBar == mSeekBarPullCloserDistance) {
			beyondar.setPullCloserDistance(progress);
		} else if (seekBar == mSeekBarPushAwayDistance) {
			beyondar.setPushAwayDistance(progress);
		} else if (seekBar == mSeekBarMaxDistanceToRender) {
			beyondar.setMaxDistanceToRender(progress);
		} else if (seekBar == mSeekBarDistanceFactor) {
			beyondar.setDistanceFactor(progress);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		adjustForCollisions();
		Log.d(TAG, "onStopTrackingTouch()");
	}

	public void initRealWorld(Double startLat, Double startLon,
			ArrayList<Searchable> worldPoints) {
		if (world != null) {
			return;
		}

		world = new World(this);
		world.setDefaultImage(R.drawable.place);
		world.setGeoPosition(startLat, startLon);
		Log.i(TAG, String.format(Locale.getDefault(),
				"World position - %10.6f %10.6f", startLat, startLon));

		for (int i = 0; i < worldPoints.size(); i++) {
			Searchable searchable = worldPoints.get(i);
			Double placeLat = searchable.getLatitude();
			if (placeLat == null || placeLat == 0) {
				continue;
			}

			Double placeLon = searchable.getLongitude();
			if (placeLon == null || placeLon == 0) {
				continue;
			}

			long id = i + 1;
			ImageObject imageObject = new ImageObject(id, searchable.getId());
			imageObject.setGeoPosition(placeLat, placeLon);
			imageObject.setName(searchable.getName());
			imageObject.setThumbUri(searchable.getImageOneUrl());
			world.addBeyondarObject(imageObject);
		}
		Log.i(TAG, world.getBeyondarObjectList(World.LIST_TYPE_DEFAULT).size()
				+ " world points was added");
	}

	public void finishActivity(View view) {
		lockOrientation(false);
		finish();
	}

	@Override
	protected void onDestroy() {
		if (imageWorkerList != null) {
			for (BeyondarImageWorker imageWorker : imageWorkerList) {
				imageWorker.cancel(true);
			}
		}
		super.onDestroy();
	}

	private void updateTextValues() {
		Log.i(TAG,
				"dst=" + beyondar.getDistanceFactor() + " max dst render="
						+ beyondar.getMaxDistanceToRender() + "\npull closer="
						+ beyondar.getPullCloserDistance() + " push away="
						+ beyondar.getPushAwayDistance());
	}

	/**
	 * Get the path to store temporally the images. Remember that you need to
	 * set WRITE_EXTERNAL_STORAGE permission in your manifest in order to
	 * write/read the storage
	 */
	private String getTmpPath() {
		return getExternalFilesDir(null).getAbsoluteFile() + "/tmp/";
	}

	/** Clean all the generated files */
	private void cleanTempFolder() {
		File tmpFolder = new File(getTmpPath());
		if (tmpFolder.isDirectory()) {
			String[] children = tmpFolder.list();
			for (int i = 0; i < children.length; i++) {
				if (children[i].startsWith(TMP_IMAGE_PREFIX)) {
					new File(tmpFolder, children[i]).delete();
				}
			}
		}
	}

	private void replaceImagesByStaticViews(World world) {
		Double worldLat = world.getLatitude();
		Double worldLon = world.getLongitude();
		imageWorkerList = new ArrayList<BeyondarImageWorker>();
		String path = getTmpPath();
		for (BeyondarObjectList beyondarList : world.getBeyondarObjectLists()) {
			for (BeyondarObject beyondarObject : beyondarList) {
				ImageObject imageObject = (ImageObject) beyondarObject;
				// First let's get the view, inflate it and change some stuff
				View view = getLayoutInflater().inflate(
						R.layout.beyondar_object_view, null);

				TextView locationName = (TextView) view
						.findViewById(R.id.location_name);
				locationName.setText(imageObject.getName());

				double kilometers = MetricUtils.haversineDistance(worldLat,
						worldLon, imageObject.getLatitude(),
						imageObject.getLongitude());
				double miles = MetricUtils.kilometersToMiles(kilometers);
				String distanceText = String.format(Locale.getDefault(),
						"%.2f km / %.2f mi", kilometers, miles);

				imageObject.setDistanceFromUser(kilometers);
				TextView locationDistance = (TextView) view
						.findViewById(R.id.location_distance);
				locationDistance.setText(distanceText);

				try {
					String imageName = TMP_IMAGE_PREFIX + imageObject.getId()
							+ ".png";
					ImageUtils.storeView(view, path, imageName);
					imageObject.setImageUri(path + imageName);
				} catch (IOException e) {
					Log.e(TAG,
							"replaceImagesByStaticViews() error - "
									+ e.getMessage());
				}

				String thumbUri = imageObject.getThumbUri();
				if (!TextUtils.isEmpty(thumbUri)) {
					BeyondarImageWorker imageWorker = new BeyondarImageWorker(
							path, thumbUri, beyondarObject, view, this);
					imageWorker.execute();
					imageWorkerList.add(imageWorker);
				}
			}
		}
	}

	private void initTestWorld() {
		if (world != null) {
			return;
		}

		world = new World(this);
		world.setDefaultImage(R.drawable.ic_launcher);
		world.setGeoPosition(41.90533734214473d, 2.565848038959814d);

		// Create an object with an image in the app resources.
		GeoObject go1 = new GeoObject(1l);
		go1.setGeoPosition(41.90523339794433d, 2.565036406654116d);
		go1.setImageResource(R.drawable.ic_launcher);
		go1.setName("Creature 1");

		// Is it also possible to load the image asynchronously form internet
		GeoObject go2 = new GeoObject(2l);
		go2.setGeoPosition(41.90518966360719d, 2.56582424468222d);
		go2.setImageResource(R.drawable.ic_launcher);
		go2.setName("Online image");

		// Also possible to get images from the SDcard
		GeoObject go3 = new GeoObject(3l);
		go3.setGeoPosition(41.90550959641445d, 2.565873388087619d);
		go3.setImageResource(R.drawable.ic_launcher);
		go3.setName("IronMan from sdcard");

		// And the same goes for the app assets
		GeoObject go4 = new GeoObject(4l);
		go4.setGeoPosition(41.90518862002349d, 2.565662767707665d);
		go4.setImageResource(R.drawable.ic_launcher);
		go4.setName("Image from assets");

		GeoObject go5 = new GeoObject(5l);
		go5.setGeoPosition(41.90553066234138d, 2.565777906882577d);
		go5.setImageResource(R.drawable.ic_launcher);
		go5.setName("Creature 5");

		GeoObject go6 = new GeoObject(6l);
		go6.setGeoPosition(41.90596218466268d, 2.565250806050688d);
		go6.setImageResource(R.drawable.ic_launcher);
		go6.setName("Creature 6");

		GeoObject go7 = new GeoObject(7l);
		go7.setGeoPosition(41.90581776104766d, 2.565932313852319d);
		go7.setImageResource(R.drawable.ic_launcher);
		go7.setName("Creature 2");

		GeoObject go8 = new GeoObject(8l);
		go8.setGeoPosition(41.90534261025682d, 2.566164369775198d);
		go8.setImageResource(R.drawable.ic_launcher);
		go8.setName("Object 8");

		GeoObject go9 = new GeoObject(9l);
		go9.setGeoPosition(41.90530734214473d, 2.565808038959814d);
		go9.setImageResource(R.drawable.ic_launcher);
		go9.setName("Creature 4");

		GeoObject go10 = new GeoObject(10l);
		go10.setGeoPosition(42.006667d, 2.705d);
		go10.setImageResource(R.drawable.ic_launcher);
		go10.setName("Far away");

		// Add the GeoObjects to the world
		world.addBeyondarObject(go1);
		world.addBeyondarObject(go2);
		world.addBeyondarObject(go3);
		world.addBeyondarObject(go4);
		world.addBeyondarObject(go5);
		world.addBeyondarObject(go6);
		world.addBeyondarObject(go7);
		world.addBeyondarObject(go8);
		world.addBeyondarObject(go9);
		world.addBeyondarObject(go10);
	}

	private List<BeyondarObject> getRadarPoints() {
		List<BeyondarObject> objectList = new ArrayList<BeyondarObject>();
		double maxDistance = radarPlugin.getMaxDistance();
		for (int i = 0; i < world.getBeyondarObjectLists().size(); i++) {
			BeyondarObjectList list = world.getBeyondarObjectList(i);
			for (int j = 0; j < list.size(); j++) {
				BeyondarObject beyondarObject = list.get(j);
				RadarPointPlugin radarPointPlugin = (RadarPointPlugin) beyondarObject
						.getFirstPlugin(RadarPointPlugin.class);
				if (radarPointPlugin.getGeoObject().getDistanceFromUser() < maxDistance
						&& radarPointPlugin.getGeoObject().isVisible()) {
					objectList.add(beyondarObject);
				}
			}
		}
		return objectList;
	}

	private void adjustForCollisions() {
		int collisions = 1;
		List<BeyondarObject> objectList = getRadarPoints();
		for (BeyondarObject first : objectList) {
			for (BeyondarObject second : objectList) {
				if (first == second) {
					continue;
				}

				if (overlap(first, second)) {
					int max = Math.max(first.getTexture().getImageWidth(),
							first.getTexture().getImageHeight());
					Point3 secondPosition = second.getPosition();
					secondPosition.y += collisions * max;
					second.setPosition(secondPosition);
					collisions++;
				}
			}
		}
	}

	private boolean overlap(BeyondarObject first, BeyondarObject second) {
		Point3 topLeft = second.getScreenPositionTopLeft();
		Point3 bottomLeft = second.getScreenPositionBottomLeft();
		Point3 bottomRight = second.getScreenPositionBottomRight();
		Point3 topRight = second.getScreenPositionTopRight();

		// Generate the collision detector
		MeshCollider collider = new SquareMeshCollider(topLeft, bottomLeft,
				bottomRight, topRight);

		return collider.contains(first.getScreenPositionBottomLeft())
				|| collider.contains(first.getScreenPositionBottomRight())
				|| collider.contains(first.getScreenPositionTopLeft())
				|| collider.contains(first.getScreenPositionTopRight());
	}

	private int getCurrentOrientation() {
		Display d = ((WindowManager) getSystemService(WINDOW_SERVICE))
				.getDefaultDisplay();
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int screenWidth = displaymetrics.widthPixels;
		int screenHeight = displaymetrics.heightPixels;
		boolean isWide = screenWidth >= screenHeight;
		switch (d.getRotation()) {
		case Surface.ROTATION_0:
			return isWide ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
					: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		case Surface.ROTATION_90:
			return isWide ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
					: ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
		case Surface.ROTATION_180:
			return isWide ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
					: ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
		case Surface.ROTATION_270:
			return isWide ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
					: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		}
		return -1;
	}

	private void lockOrientation(boolean lock) {
		if (lock) {
			setRequestedOrientation(getCurrentOrientation());
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
		}
	}

	private class ImageObject extends GeoObject {

		private String objectId;
		private String thumbUri;

		public ImageObject(long id, String objectId) {
			super(id);
			this.objectId = objectId;
		}

		public String getThumbUri() {
			return thumbUri;
		}

		public void setThumbUri(String thumbUri) {
			this.thumbUri = thumbUri;
		}

		public String getObjectId() {
			return objectId;
		}
	}

	private final static class BeyondarImageWorker extends
			AsyncTask<Void, Void, Bitmap> {

		private final String path;
		private final String thumbUri;
		private final ImageObject imageObject;
		private final View container;
		private final HeadsUpActivity parent;

		private BeyondarImageWorker(String path, String thumbUri,
				BeyondarObject beyondarObject, View container, HeadsUpActivity parent) {
			this.path = path;
			this.thumbUri = thumbUri;
			this.imageObject = (ImageObject) beyondarObject;
			this.container = container;
			this.parent = parent;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap bitmap = null;
			if (isCancelled()) {
				return null;
			}

			ImageLoader imageLoader = ImageLoader.getInstance(Daytripper
					.getAppContext());
			bitmap = imageLoader.getBitmapFromCache(thumbUri);
			if (bitmap != null) {
				return bitmap;
			}
			bitmap = imageLoader.fetchBitmap(thumbUri);
			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			try {
				if (bitmap != null) {
					ImageView imageView = (ImageView) container
							.findViewById(R.id.location_img);
					imageView.setImageBitmap(bitmap);
					imageView.setVisibility(View.VISIBLE);
				} else {
					return;
				}

				String oldView = TMP_IMAGE_PREFIX + imageObject.getId()
						+ ".png";
				File oldViewFile = new File(path, oldView);
				if (oldViewFile.exists()) {
					oldViewFile.delete();
				}

				// Now that we have it we need to store this view in the
				// storage in order to allow the framework to load it when
				// it will be need it
				String newView = TMP_IMAGE_PREFIX + imageObject.getObjectId()
						+ ".png";
				parent.storeView(container, new File(path), newView);

				// If there are no errors we can tell the object to use the
				// view that we just stored
				imageObject.setImageUri(path + newView);
			} catch (IOException e) {
				Log.e(TAG, "onPostExecute() error - " + e.getMessage());
			}
		}
	}

	@Override
	public void onTouchBeyondarView(MotionEvent event,
			BeyondarGLSurfaceView beyondarView) {
	}

	@Override
	public void onClickBeyondarObject(ArrayList<BeyondarObject> beyondarObjects) {
		for (BeyondarObject geoObject : beyondarObjects) {
			Log.i(TAG, String.format(Locale.getDefault(), "%s -> {%s, %s}",
					geoObject.getName(), geoObject.getPosition().toString(),
					geoObject.getScreenPositionCenter().toString()));
		}
	}

	private Bitmap getBitmapFromView(View view) {
		view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
		view.setDrawingCacheEnabled(true);
	    view.buildDrawingCache();
	    
	    BitmapDrawable drawable = new BitmapDrawable(getResources(), view.getDrawingCache());
	    drawable.setAntiAlias(true);
	    return drawable.getBitmap();
	}

	public void storeView(View view, File path, String fileName)
			throws IOException {
		if (!path.exists()) {
			path.mkdirs();
		}
		
		FileOutputStream file = new FileOutputStream(new File(path, fileName));
		Bitmap bitmap = getBitmapFromView(view);
		bitmap.compress(CompressFormat.PNG, 100, file);
		file.close();
		bitmap.recycle();
	}
}
