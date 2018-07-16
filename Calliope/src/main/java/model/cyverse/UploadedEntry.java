package model.cyverse;

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
	// A path to the upload on CyVerse
	private String uploadIRODSPath;

	/**
	 * Constructor initializes all fields
	 * Constructor initializes all fields
	 *
	 * @param uploadUser The user that uploaded the images
	 * @param uploadDate The date the upload happened on
	 * @param uploadIRODSPath The path to the file on CyVerse
	 */
	public UploadedEntry(String uploadUser, LocalDateTime uploadDate, Integer imageCount, String uploadIRODSPath)
	{
		this.uploadUser = uploadUser;
		this.uploadDate = uploadDate;
		this.imageCount = imageCount;
		this.uploadIRODSPath = uploadIRODSPath;
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

	public String getUploadIRODSPath()
	{
		return uploadIRODSPath;
	}
}
