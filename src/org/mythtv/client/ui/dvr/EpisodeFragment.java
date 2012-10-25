package org.mythtv.client.ui.dvr;

import org.mythtv.R;
import org.mythtv.client.ui.AbstractMythFragment;
import org.mythtv.db.dvr.ProgramConstants;

import android.content.ContentUris;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EpisodeFragment  extends AbstractMythFragment {

	private static final String TAG = ProgramGroupActivity.class.getSimpleName();
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View root = inflater.inflate( R.layout.fragment_dvr_episode, container, false );
		
		return root;
	}

	
	
	public void loadEpisode(long id){
		
		String[] projection =
		{ 
				ProgramConstants._ID, ProgramConstants.FIELD_TITLE, 
				ProgramConstants.FIELD_SUB_TITLE,
				ProgramConstants.FIELD_DESCRIPTION,
				ProgramConstants.FIELD_AIR_DATE,
				ProgramConstants.FIELD_CHANNEL_NUMBER,
				ProgramConstants.FIELD_CATEGORY,
				ProgramConstants.FIELD_START_TIME, 
				ProgramConstants.FIELD_END_TIME
		};
		
		Cursor cursor = getActivity().getContentResolver().query(
				ContentUris.withAppendedId( ProgramConstants.CONTENT_URI_RECORDED, id ),
				projection,
				null, null, null );
		if( cursor.moveToFirst() ) {
			
			String title = cursor.getString( cursor.getColumnIndexOrThrow( ProgramConstants.FIELD_TITLE ));
	        Log.d( TAG, "loadEpisode : Episode_Title=" + title );
	        
	        //get activity to grab views from
	        FragmentActivity activity = this.getActivity();
	        
	        //title
	        TextView tView = (TextView)activity.findViewById(R.id.textView_episode_title);
	        tView.setText(title);
	        
	        //subtitle
	        tView = (TextView)activity.findViewById(R.id.textView_episode_subtitle);
	        tView.setText(cursor.getString( cursor.getColumnIndexOrThrow( ProgramConstants.FIELD_SUB_TITLE )));
	        
	        //description
	        tView = (TextView)activity.findViewById(R.id.textView_episode_description);
	        tView.setText(cursor.getString( cursor.getColumnIndexOrThrow( ProgramConstants.FIELD_DESCRIPTION )));
	        
	        //channel number
	        tView = (TextView)activity.findViewById(R.id.textView_episode_ch_num);
	        tView.setText(cursor.getString( cursor.getColumnIndexOrThrow( ProgramConstants.FIELD_CHANNEL_NUMBER )));
	        
	        //airdate
	        tView = (TextView)activity.findViewById(R.id.textView_episode_airdate);
	        tView.setText(cursor.getString( cursor.getColumnIndexOrThrow( ProgramConstants.FIELD_AIR_DATE )));
	        
		}else{
			Log.d( TAG, "loadEpisode: Empty Cursor Returned" );
		}
		cursor.close();
	}
	
}