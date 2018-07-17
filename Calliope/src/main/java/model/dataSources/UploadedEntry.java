package model.dataSources;

import java.time.LocalDateTime;

/**
 * The cloud upload entry that represents some upload at some point in time
 */
public class UploadedEntry
{
	// The username of the person that uploaded images
	private String uploadUser;
	// The date the upload happened on
	private LocalDateTime uploadDate;
	// The number of images in this upload
	private Integer imageCount;
	// A path to the upload
	private String uploadPath;
	// The storage method for the upload
	private String storageMethod;

	/**
	 * Constructor initializes all fields
	 * Constructor initializes all fields
	 *
	 * @param uploadUser The user that uploaded the images
	 * @param uploadDate The date the upload happened on
	 * @param imageCount The number of images in the upload
	 * @param uploadPath The path to the file
	 * @param storageMethod How the image is stored on the system
	 */
	public UploadedEntry(String uploadUser, LocalDateTime uploadDate, Integer imageCount, String uploadPath, String storageMethod)
	{
		this.uploadUser = uploadUser;
		this.uploadDate = uploadDate;
		this.imageCount = imageCount;
		this.uploadPath = uploadPath;
		this.storageMethod = storageMethod;
	}

	///
	/// Getters
	///

	public String getUploadUser()
	{
		return uploadUser;
	}

	public LocalDateTime getUploadDate()
	{
		return uploadDate;
	}

	public Integer getImageCount()
	{
		return imageCount;
	}

	public String getUploadPath()
	{
		return uploadPath;
	}

	public String getStorageMethod()
	{
		return storageMethod;
	}
}
