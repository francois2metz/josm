// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.tools.ListenerList;

/**
 * Class to hold {@link ImageEntry} and the current selection
 * @since xxx
 */
public class ImageData {
    /**
     * A listener that is informed when the current selection change
     */
    public interface ImageDataUpdateListener {
        /**
         * Called when the data change
         * @param data the image data
         */
        void imageDataUpdated(ImageData data);

        /**
         * Called when the selection change
         * @param data the image data
         */
        void selectedImageChanged(ImageData data);
    }

    private final List<ImageEntry> data;

    private int selectedImageIndex = -1;

    private final ListenerList<ImageDataUpdateListener> listeners = ListenerList.create();

    /**
     * Construct a new image container without images
     */
    public ImageData() {
        this(null);
    }

    /**
     * Construct a new image container with a list of images
     * @param data the list of {@link ImageEntry}
     */
    public ImageData(List<ImageEntry> data) {
        if (data != null) {
            Collections.sort(data);
            this.data = data;
        } else {
            this.data = new ArrayList<>();
        }
    }

    /**
     * Returns the images
     * @return the images
     */
    public List<ImageEntry> getImages() {
        return this.data;
    }

    /**
     * Determines if one image has modified GPS data.
     * @return {@code true} if data has been modified; {@code false}, otherwise
     */
    public boolean isModified() {
        for (ImageEntry e : data) {
            if (e.hasNewGpsData()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Merge 2 ImageData
     * @param data {@link ImageData}
     */
    public void mergeFrom(ImageData data) {
        this.data.addAll(data.getImages());
        Collections.sort(this.data);

        final ImageEntry selected = data.getSelectedImage();

        // Suppress the double photos.
        if (this.data.size() > 1) {
            ImageEntry cur;
            ImageEntry prev = this.data.get(this.data.size() - 1);
            for (int i = this.data.size() - 2; i >= 0; i--) {
                cur = this.data.get(i);
                if (cur.getFile().equals(prev.getFile())) {
                    this.data.remove(i);
                } else {
                    prev = cur;
                }
            }
        }
        if (selected != null) {
            this.setSelectedImageIndex(this.data.indexOf(selected));
        }
    }

    /**
     * Return the current selected image
     * @return the selected image as {@link ImageEntry} or null
     */
    public ImageEntry getSelectedImage() {
        if (this.selectedImageIndex > -1) {
            return data.get(this.selectedImageIndex);
        }
        return null;
    }

    /**
     * Select the first image of the sequence
     */
    public void selectFirstImage() {
        if (!data.isEmpty()) {
            this.setSelectedImageIndex(0);
        }
    }

    /**
     * Select the last image of the sequence
     */
    public void selectLastImage() {
        this.setSelectedImageIndex(data.size() - 1);
    }

    /**
     * Check if there is a next image in the sequence
     * @return {@code true} is there is a next image, {@code false} otherwise
     */
    public boolean hasNextImage() {
        return (this.selectedImageIndex != data.size() - 1);
    }

    /**
     * Select the next image of the sequence
     */
    public void selectNextImage() {
        if (this.hasNextImage()) {
            this.setSelectedImageIndex(this.selectedImageIndex + 1);
        }
    }

    /**
     *  Check if there is a previous image in the sequence
     * @return {@code true} is there is a previous image, {@code false} otherwise
     */
    public boolean hasPreviousImage() {
        return this.selectedImageIndex - 1 > -1;
    }

    /**
     * Select the previous image of the sequence
     */
    public void selectPreviousImage() {
        if (data.isEmpty()) {
            return;
        }
        this.setSelectedImageIndex(Integer.max(0, this.selectedImageIndex - 1));
    }

    /**
     * Select as the selected the given image
     * @param image the selected image
     */
    public void setSelectedImage(ImageEntry image) {
        this.setSelectedImageIndex(this.data.indexOf(image));
    }

    /**
     * Clear the selected image
     */
    public void clearSelectedImage() {
        this.setSelectedImageIndex(-1);
    }

    private void setSelectedImageIndex(int index) {
        this.setSelectedImageIndex(index, false);
    }

    private void setSelectedImageIndex(int index, boolean forceTrigger) {
        if (index == this.selectedImageIndex && !forceTrigger) {
            return;
        }
        this.selectedImageIndex = index;
        listeners.fireEvent(l -> l.selectedImageChanged(this));
    }

    /**
     * Remove the current selected image from the list
     */
    public void removeSelectedImage() {
        data.remove(this.getSelectedImage());
        if (this.selectedImageIndex == data.size()) {
            this.setSelectedImageIndex(data.size() - 1);
        } else {
            this.setSelectedImageIndex(this.selectedImageIndex, true);
        }
    }

    /**
     * Remove the image from the list and trigger update listener
     * @param img the {@link ImageEntry} to remove
     */
    public void removeImage(ImageEntry img) {
        data.remove(img);
        this.notifyImageUpdate();
    }

    /**
     * Update the position of the image and trigger update
     * @param img the image to update
     * @param newPos the new position
     */
    public void updateImagePosition(ImageEntry img, LatLon newPos) {
        img.setPos(newPos);
        this.afterImageUpdated(img);
    }

    /**
     * Update the image direction of the image and trigger update
     * @param img the image to update
     * @param direction the new direction
     */
    public void updateImageDirection(ImageEntry img, double direction) {
        img.setExifImgDir(direction);
        this.afterImageUpdated(img);
    }

    /**
     * Manually trigger the {@link ImageDataUpdateListener#imageDataUpdated(ImageData)}
     */
    public void notifyImageUpdate() {
        listeners.fireEvent(l -> l.imageDataUpdated(this));
    }

    private void afterImageUpdated(ImageEntry img) {
        img.flagNewGpsData();
        this.notifyImageUpdate();
    }

    /**
     * Add a listener that listens to image data changes
     * @param listener the {@link ImageDataUpdateListener}
     */
    public void addImageDataUpdateListener(ImageDataUpdateListener listener) {
        listeners.addListener(listener);
    }

    /**
     * Removes a listener that listens to image data changes
     * @param listener The listener
     */
    public void removeImageDataUpdateListener(ImageDataUpdateListener listener) {
        listeners.removeListener(listener);
    }
}
