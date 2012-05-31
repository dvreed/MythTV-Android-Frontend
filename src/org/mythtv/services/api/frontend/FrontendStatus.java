/**
 *  This file is part of MythTV for Android
 * 
 *  MythTV for Android is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MythTV for Android is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MythTV for Android.  If not, see <http://www.gnu.org/licenses/>.
 *   
 * @author Daniel Frey <dmfrey at gmail dot com>
 * 
 * This software can be found at <https://github.com/dmfrey/mythtv-for-android/>
 *
 */
package org.mythtv.services.api.frontend;

import java.util.List;

/**
 * @author Daniel Frey
 *
 */
public class FrontendStatus {

	private List<State> states;
	
	public FrontendStatus() { }

	/**
	 * @return the states
	 */
	public List<State> getStates() {
		return states;
	}

	/**
	 * @param states the states to set
	 */
	public void setStates( List<State> states ) {
		this.states = states;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		
		builder.append( "FrontendStatus [" );
		
		if( states != null ) {
			builder.append( "states=" );
			builder.append( states );
		}
		
		builder.append( "]" );
	
		return builder.toString();
	}
	
}