/**
 * This file is part of MythTV Android Frontend
 *
 * MythTV Android Frontend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MythTV Android Frontend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MythTV Android Frontend.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This software can be found at <https://github.com/MythTV-Clients/MythTV-Android-Frontend/>
 */
package org.mythtv.service.dvr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.mythtv.R;
import org.mythtv.client.ui.preferences.LocationProfile;
import org.mythtv.db.dvr.RecordingRuleDaoHelper;
import org.mythtv.db.http.EtagDaoHelper;
import org.mythtv.db.http.EtagInfoDelegate;
import org.mythtv.db.preferences.LocationProfileDaoHelper;
import org.mythtv.service.MythtvService;
import org.mythtv.service.util.FileHelper;
import org.mythtv.service.util.NetworkHelper;
import org.mythtv.services.api.dvr.RecRuleList;
import org.mythtv.services.api.dvr.RecRules;
import org.mythtv.services.api.dvr.impl.DvrTemplate.Endpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author Daniel Frey
 *
 */
public class RecordingRuleDownloadService extends MythtvService {

	private static final String TAG = RecordingRuleDownloadService.class.getSimpleName();

	private static final String RECORDING_RULE_FILE_PREFIX = "recording_rule_";
	private static final String RECORDING_RULE_FILE_EXT = ".json";
	
    public static final String ACTION_DOWNLOAD = "org.mythtv.background.recordingRuleDownload.ACTION_DOWNLOAD";
    public static final String ACTION_PROGRESS = "org.mythtv.background.recordingRuleDownload.ACTION_PROGRESS";
    public static final String ACTION_COMPLETE = "org.mythtv.background.recordingRuleDownload.ACTION_COMPLETE";

    public static final String EXTRA_PROGRESS = "PROGRESS";
    public static final String EXTRA_PROGRESS_DATA = "PROGRESS_DATA";
    public static final String EXTRA_PROGRESS_ERROR = "PROGRESS_ERROR";
    public static final String EXTRA_COMPLETE = "COMPLETE";
    public static final String EXTRA_COMPLETE_UPTODATE = "COMPLETE_UPTODATE";
    public static final String EXTRA_COMPLETE_OFFLINE = "COMPLETE_OFFLINE";
    
	private NotificationManager mNotificationManager;
	private int notificationId;
	
	private File recordingRuleDirectory = null;
	private RecordingRuleDaoHelper mRecordingRuleDaoHelper = RecordingRuleDaoHelper.getInstance();
	private EtagDaoHelper mEtagDaoHelper = EtagDaoHelper.getInstance();
	private LocationProfileDaoHelper mLocationProfileDaoHelper = LocationProfileDaoHelper.getInstance();
	
	public RecordingRuleDownloadService() {
		super( "RecordingRuleDownloadService" );
	}
	
	/* (non-Javadoc)
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent( Intent intent ) {
		Log.d( TAG, "onHandleIntent : enter" );
		super.onHandleIntent( intent );
		
		boolean passed = true;
		
		recordingRuleDirectory = FileHelper.getInstance().getProgramRecordedDataDirectory();
		if( null == recordingRuleDirectory || !recordingRuleDirectory.exists() ) {
			Intent completeIntent = new Intent( ACTION_COMPLETE );
			completeIntent.putExtra( EXTRA_COMPLETE, "Recording Rule location can not be found" );
			sendBroadcast( completeIntent );

			Log.d( TAG, "onHandleIntent : exit, recordingRuleDirectory does not exist" );
			return;
		}

		LocationProfile locationProfile = mLocationProfileDaoHelper.findConnectedProfile( this );
		if( !NetworkHelper.getInstance().isMasterBackendConnected( this, locationProfile ) ) {
			Intent completeIntent = new Intent( ACTION_COMPLETE );
			completeIntent.putExtra( EXTRA_COMPLETE, "Master Backend unreachable" );
			completeIntent.putExtra( EXTRA_COMPLETE_OFFLINE, Boolean.TRUE );
			sendBroadcast( completeIntent );

			Log.d( TAG, "onHandleIntent : exit, Master Backend unreachable" );
			return;
		}
		
		mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );

		if ( intent.getAction().equals( ACTION_DOWNLOAD ) ) {
    		Log.i( TAG, "onHandleIntent : DOWNLOAD action selected" );

    		RecRules recordingRules = null;
    		try {
    			sendNotification();

    			download( locationProfile );

			} catch( Exception e ) {
				Log.e( TAG, "onHandleIntent : error", e );
				
				passed = false;
			} finally {
    			completed();

    			Intent completeIntent = new Intent( ACTION_COMPLETE );
    			completeIntent.putExtra( EXTRA_COMPLETE, "Recording Rules Download Service Finished" );
    			if( null == recordingRules ) {
    				completeIntent.putExtra( EXTRA_COMPLETE_UPTODATE, passed );
    			}
    			
    			sendBroadcast( completeIntent );
    		}
    		
        }
		
		Log.d( TAG, "onHandleIntent : exit" );
	}

	// internal helpers
	
	private void download( final LocationProfile locationProfile ) throws Exception {
		Log.v( TAG, "download : enter" );

		EtagInfoDelegate etag = mEtagDaoHelper.findByEndpointAndDataId( this, locationProfile, Endpoint.GET_RECORD_SCHEDULE_LIST.name(), "" );
		
		ResponseEntity<RecRuleList> responseEntity = mMythtvServiceHelper.getMythServicesApi( locationProfile ).dvrOperations().getRecordScheduleList( -1, -1, etag );

		if( responseEntity.getStatusCode().equals( HttpStatus.OK ) ) {
			Log.i( TAG, "download : " + Endpoint.GET_RECORD_SCHEDULE_LIST.getEndpoint() + " returned 200 OK" );
			RecRuleList recRuleList = responseEntity.getBody();

			if( null != recRuleList.getRecRules() ) {

				process( recRuleList.getRecRules(), locationProfile );	

				if( null != etag.getValue() ) {
					Log.i( TAG, "download : saving etag: " + etag.getValue() );

					etag.setEndpoint( Endpoint.GET_RECORD_SCHEDULE_LIST.name() );
					etag.setDate( new DateTime() );
					etag.setMasterHostname( locationProfile.getHostname() );
					etag.setLastModified( new DateTime() );
					mEtagDaoHelper.save( this, locationProfile, etag );
				}

			}

		}

		if( responseEntity.getStatusCode().equals( HttpStatus.NOT_MODIFIED ) ) {
			Log.i( TAG, "download : " + Endpoint.GET_RECORD_SCHEDULE_LIST.getEndpoint() + " returned 304 Not Modified" );

			if( null != etag.getValue() ) {
				etag.setDate( new DateTime() );
				etag.setLastModified( new DateTime() );
				mEtagDaoHelper.save( this, locationProfile, etag );
			}

		}
			
		Log.v( TAG, "download : exit" );
	}

	private void cleanup() throws IOException {
		Log.v( TAG, "cleanup : enter" );
		
		FileUtils.cleanDirectory( recordingRuleDirectory );
		
		Log.v( TAG, "cleanup : exit" );
	}

	private void process( RecRules recRules, final LocationProfile locationProfile ) throws JsonGenerationException, JsonMappingException, IOException, RemoteException, OperationApplicationException {
		Log.v( TAG, "process : enter" );
		
		Log.v( TAG, "process : saving recording rules for host [" + locationProfile.getHostname() + ":" + locationProfile.getUrl() + "]" );
		
		mMainApplication.getObjectMapper().writeValue( new File( recordingRuleDirectory, RECORDING_RULE_FILE_PREFIX + locationProfile.getHostname() + RECORDING_RULE_FILE_EXT ), recRules );
		Log.v( TAG, "process : saved recorded to " + recordingRuleDirectory.getAbsolutePath() );

		int recordingRulesAdded = mRecordingRuleDaoHelper.load( this, locationProfile, recRules.getRecRules() );
		Log.v( TAG, "process : recordingRulesAdded=" + recordingRulesAdded );
		
		Log.v( TAG, "process : exit" );
	}

	// internal helpers
	
	@SuppressWarnings( "deprecation" )
	private void sendNotification() {

		long when = System.currentTimeMillis();
		notificationId = (int) when;
		
        Notification mNotification = new Notification( android.R.drawable.stat_notify_sync, getResources().getString( R.string.notification_sync_recording_rules ), when );

        Intent notificationIntent = new Intent();
        PendingIntent mContentIntent = PendingIntent.getActivity( this, 0, notificationIntent, 0 );

        mNotification.setLatestEventInfo( this, getResources().getString( R.string.app_name ), getResources().getString( R.string.notification_sync_recording_rules ), mContentIntent );

        mNotification.flags = Notification.FLAG_ONGOING_EVENT;

        mNotificationManager.notify( notificationId, mNotification );
	
	}
	
    public void completed()    {
        
    	if( null != mNotificationManager ) {
        	mNotificationManager.cancel( notificationId );
    	}
    
    }

}
