/**
 * StationListProvider.java
 * Implements the StationListProvider class
 * A StationListProvider provides a list of stations as MediaMetadata items
 * Credit: https://github.com/googlesamples/android-MediaBrowserService/ (-> MusicProvider)
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.media.MediaMetadataCompat;

import org.y20k.transistor.core.Station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;


/**
 * StationListProvider.class
 */
public class StationListProvider implements TransistorKeys {

    /* Define log tag */
    private static final String LOG_TAG = StationListProvider.class.getSimpleName();


    /* Callback used by PlayerService */
    public interface Callback {
        void onStationListReady(boolean success);
    }


    /* Main class variables */
    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_EMPTY_ROOT = "__EMPTY__";
    private final LinkedHashMap<String, MediaMetadataCompat> mStationListById;
    private enum State { NON_INITIALIZED, INITIALIZING, INITIALIZED }
    private volatile State mCurrentState = State.NON_INITIALIZED;


    /* Constructor */
    public StationListProvider() {
        mStationListById = new LinkedHashMap<>();
    }


    /* Return list of all stations */
    public Iterable<MediaMetadataCompat> getAllMusics() {
        if (mCurrentState != State.INITIALIZED || mStationListById.isEmpty()) {
            return Collections.emptyList();
        }
        return mStationListById.values();
    }


    /* Return the first station in list */
    public MediaMetadataCompat getFirstStation() {
        MediaMetadataCompat firstStationMetaData = null;
        firstStationMetaData = mStationListById.entrySet().iterator().next().getValue();

        if (firstStationMetaData != null) {
            return firstStationMetaData;
        } else {
            // stupid hack: only if mStationListById is empty
            String stationName = "KCSB";
            String stationUri = "http://live.kcsb.org:80/KCSB_128\n";
            return new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(stationUri.hashCode()))
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, stationUri)
                    .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Radio")
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, stationName)
//                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, stationName)
//                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, stationName)
//                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
//                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
//                    .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
//                    .putString(METADATA_CUSTOM_KEY_IMAGE_FILE, station.getStationImageFile().getPath())
//                    .putString(METADATA_CUSTOM_KEY_PLAYLIST_FILE, station.getStationPlaylistFile().getPath())
                    .build();
        }
    }


    /* Return the MediaMetadata for given ID */
    public MediaMetadataCompat getStationMediaMetadata(String stationId) {
        return mStationListById.containsKey(stationId) ? mStationListById.get(stationId) : null;
    }


    /* Update metadata associated with stationId */
    public synchronized void updateMusic(String stationId, MediaMetadataCompat metadata) {
        MediaMetadataCompat track = mStationListById.get(stationId);
        if (track != null) {
            mStationListById.put(stationId, metadata);
        }
    }


    /* Return current state */
    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }


    /* Gets list of stations and caches the track information */
    public void retrieveMediaAsync(final Context context, final Callback callback) {
        LogHelper.v(LOG_TAG, "retrieveMediaAsync called");
        if (mCurrentState == State.INITIALIZED) {
            // already initialized, so call back immediately.
            callback.onStationListReady(true);
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveStations(context);
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(State current) {
                if (callback != null) {
                    callback.onStationListReady(current == State.INITIALIZED);
                }
            }
        }.execute();
    }


    /* */
    private synchronized void retrieveStations(Context context) {
        if (mCurrentState == State.NON_INITIALIZED) {
            mCurrentState = State.INITIALIZING;

            ArrayList<Station> stationList = StationListHelper.loadStationListFromStorage(context);
            if (stationList != null) {
                for (Station station : stationList) {
                    MediaMetadataCompat item = buildMediaMetadata(station);
                    String mediaId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mStationListById.put(mediaId, item);
                }
            }
            mCurrentState = State.INITIALIZED;
        }
    }


    /* Creates MediaMetadata from station */
    private MediaMetadataCompat buildMediaMetadata(Station station) {

        // since we don't have a unique ID, we fake one using the hashcode of the stream address
        String id = String.valueOf(station.getStreamUri().hashCode());

        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, station.getStreamUri().toString())
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, station.getStationName())
//                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, station.getStationName())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, "Radio")
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUrl)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,  station.getStationName())
//                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNumber)
//                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                .putString(METADATA_CUSTOM_KEY_IMAGE_FILE, station.getStationImageFile().getPath())
                .putString(METADATA_CUSTOM_KEY_PLAYLIST_FILE, station.getStationPlaylistFile().getPath())
                .build();
    }

}
