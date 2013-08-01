/**
 * 
 */
package org.mythtv.client.ui.dvr;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mythtv.R;
import org.mythtv.client.MainApplication;
import org.mythtv.client.ui.preferences.LocationProfile;
import org.mythtv.client.ui.util.MythtvListFragment;
import org.mythtv.db.dvr.ProgramConstants;
import org.mythtv.db.dvr.ProgramDaoHelper;
import org.mythtv.service.util.DateUtils;
import org.mythtv.services.api.dvr.Program;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * @author dmfrey
 *
 */
public class GuideDataFragment extends MythtvListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = GuideDataFragment.class.getSimpleName();

	private MainApplication mMainApplication;
	
	private ProgramGuideChannelCursorAdapter mAdapter;
	
	private LocationProfile mLocationProfile;

	/**
	 * 
	 */
	public GuideDataFragment() { }

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int, android.os.Bundle)
	 */
	@Override
	public Loader<Cursor> onCreateLoader( int id, Bundle args ) {
		Log.v( TAG, "onCreateLoader : enter" );
		
		String[] projection = null;
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = null;
		
		switch( id ) {
		case 0 :
		    Log.v( TAG, "onCreateLoader : getting prorgrams for channel" );
			
		    int channelId = args.getInt( ProgramConstants.FIELD_CHANNEL_ID );
		    long date = args.getLong( ProgramConstants.FIELD_END_TIME );
		    
		    DateTime start = new DateTime( date );
		    Log.v( TAG, "onCreateLoader : getting prorgrams for channel " + channelId + " on " + start.toString() );
		    
			projection = new String[] { ProgramConstants._ID, ProgramConstants.FIELD_TITLE, ProgramConstants.FIELD_SUB_TITLE, ProgramConstants.FIELD_CATEGORY, ProgramConstants.FIELD_START_TIME, ProgramConstants.FIELD_END_TIME };
			selection = ProgramConstants.TABLE_NAME_GUIDE + "." + ProgramConstants.FIELD_CHANNEL_ID + " = ? AND " + ProgramConstants.TABLE_NAME_GUIDE + "." + ProgramConstants.FIELD_END_TIME + " > ? AND " + ProgramConstants.TABLE_NAME_GUIDE + "." + ProgramConstants.FIELD_START_TIME + " < ? AND " + ProgramConstants.TABLE_NAME_GUIDE + "." + ProgramConstants.FIELD_MASTER_HOSTNAME + " = ?";
			selectionArgs = new String[] { String.valueOf( channelId ), String.valueOf( start.withZone( DateTimeZone.UTC ).getMillis() ), String.valueOf( start.plusDays( 1 ).withZone( DateTimeZone.UTC ).getMillis() ), mLocationProfile.getHostname() };
			sortOrder = ProgramConstants.TABLE_NAME_GUIDE + "." + ProgramConstants.FIELD_START_TIME;

			Log.v( TAG, "onCreateLoader : exit" );
			return new CursorLoader( getActivity(), ProgramConstants.CONTENT_URI_GUIDE, projection, selection, selectionArgs, sortOrder );
			
		default : 
		    Log.v( TAG, "onCreateLoader : exit, invalid id" );

		    return null;
		}
			
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android.support.v4.content.Loader, java.lang.Object)
	 */
	@Override
	public void onLoadFinished( Loader<Cursor> loader, Cursor cursor ) {
		Log.v( TAG, "onLoadFinished : enter" );
		
		mAdapter.swapCursor( cursor );
		
		Log.v( TAG, "onLoadFinished : exit" );
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android.support.v4.content.Loader)
	 */
	@Override
	public void onLoaderReset( Loader<Cursor> loader ) {
		Log.v( TAG, "onLoaderReset : enter" );
		
		mAdapter.swapCursor( null );
		
		Log.v( TAG, "onLoaderReset : exit" );
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
	 */
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState ) {

		View view = inflater.inflate( R.layout.program_guide_data, null );
		
		return view;
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated( Bundle savedInstanceState ) {
		Log.v( TAG, "onActivityCreated : enter" );
		super.onActivityCreated( savedInstanceState );

		mMainApplication = getMainApplication();
		mLocationProfile = mLocationProfileDaoHelper.findConnectedProfile( getActivity() );
		
		mAdapter = new ProgramGuideChannelCursorAdapter( getActivity() );
	    setListAdapter( mAdapter );

	    Bundle args = getArguments();
	    	    
		getLoaderManager().initLoader( 0, args, this );

		Log.v( TAG, "onActivityCreated : exit" );
	}

	public void updateView( int channelId, DateTime date ) {
		Log.v( TAG, "updateView : enter" );
		
		Bundle args = new Bundle();
		args.putInt( ProgramConstants.FIELD_CHANNEL_ID, channelId );
		args.putLong( ProgramConstants.FIELD_END_TIME, date.getMillis() );

		getLoaderManager().restartLoader( 0, args, this );

		mAdapter.notifyDataSetChanged();
		
		Log.v( TAG, "updateView : exit" );
	}
	
	// internal helpers
	
	private class ProgramGuideChannelCursorAdapter extends CursorAdapter {
	
		private LayoutInflater mInflater;

		public ProgramGuideChannelCursorAdapter( Context context ) {
			super( context, null, false );
			
			mInflater = LayoutInflater.from( context );
		}

		/* (non-Javadoc)
		 * @see android.support.v4.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
		 */
		@Override
		public void bindView( View view, Context context, Cursor cursor ) {
			
			Program program = ProgramDaoHelper.convertCursorToProgram( cursor, ProgramConstants.TABLE_NAME_GUIDE );

			if( null != program ) {
				Log.v( TAG, "bindView : title=" + program.getTitle() + ", startTime=" + DateUtils.getDateTimeUsingLocaleFormattingPretty( program.getStartTime(), mMainApplication.getDateFormat(), mMainApplication.getClockType() ) );
				
				final ProgramViewHolder mHolder = (ProgramViewHolder) view.getTag();
	        
	        	mHolder.category.setBackgroundColor( mProgramHelper.getCategoryColor( program.getCategory() ) );
	        	mHolder.title.setText( program.getTitle() );
	        	mHolder.subTitle.setText( program.getSubTitle() );
	        	mHolder.startTime.setText( DateUtils.getTimeWithLocaleFormatting( program.getStartTime(), mMainApplication.getClockType() ) );

	        	long duration = ( program.getEndTime().getMillis() - program.getStartTime().getMillis() ) / 60000;
	        	mHolder.duration.setText( duration + " mins" );
	        	
			}
			
		}

		/* (non-Javadoc)
		 * @see android.support.v4.widget.CursorAdapter#newView(android.content.Context, android.database.Cursor, android.view.ViewGroup)
		 */
		@Override
		public View newView( Context context, Cursor cursor, ViewGroup parent ) {
			
			View view = mInflater.inflate( R.layout.program_guide_data_item, parent, false );
			
			ProgramViewHolder refHolder = new ProgramViewHolder();
			refHolder.row = (LinearLayout) view.findViewById( R.id.program_guide_data_item );
			refHolder.category = (View) view.findViewById( R.id.program_guide_data_item_category );
			refHolder.title = (TextView) view.findViewById( R.id.program_guide_data_item_title );
			refHolder.subTitle = (TextView) view.findViewById( R.id.program_guide_data_item_sub_title );
			refHolder.startTime = (TextView) view.findViewById( R.id.program_guide_data_item_start_time );
			refHolder.duration = (TextView) view.findViewById( R.id.program_guide_data_item_duration );
			
			view.setTag( refHolder );
			
			return view;
		}

	}
	
	private static class ProgramViewHolder {
		
		LinearLayout row;
		
		View category;
		TextView title;
		TextView subTitle;
		TextView startTime;
		TextView duration;
		
	}

}
