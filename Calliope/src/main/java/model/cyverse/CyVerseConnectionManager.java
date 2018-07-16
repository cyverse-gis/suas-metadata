package model.cyverse;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import model.CalliopeData;
import model.analysis.CalliopeAnalysisUtils;
import model.image.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.irods.jargon.core.connection.AuthScheme;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.connection.IRODSSession;
import org.irods.jargon.core.connection.IRODSSimpleProtocolManager;
import org.irods.jargon.core.connection.auth.AuthResponse;
import org.irods.jargon.core.exception.AuthenticationException;
import org.irods.jargon.core.exception.InvalidUserException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.protovalues.FilePermissionEnum;
import org.irods.jargon.core.pub.*;
import org.irods.jargon.core.pub.domain.User;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.pub.io.IRODSFileImpl;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * A class used to wrap the CyVerse Jargon FTP/iRODS library
 */
public class CyVerseConnectionManager
{
	// The string containing the host address that we connect to
	private static final String CYVERSE_HOST = "data.cyverse.org"; // diana.cyverse.org
	// The directory that each user has as their home directory
	private static final String HOME_DIRECTORY = "/iplant/home/";
	// The directory that collections are stored in
	private static final String COLLECTIONS_DIRECTORY = "/iplant/home/dslovikosky/Calliope/Collections";
	// Each user is part of the iPlant zone
	private static final String ZONE = "iplant";
	private static final SimpleDateFormat FOLDER_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;

	// Cache the authenticated iRODS account
	private IRODSAccount authenticatedAccount;
	// Session manager ensures that we don't leave sessions open
	private CyVerseSessionManager sessionManager;

	/**
	 * Given a username and password, this method logs a cyverse user in
	 *
	 * @param username The username of the CyVerse account
	 * @param password The password of the CyVerse account
	 * @return True if the login was successful, false otherwise
	 */
	public Boolean login(String username, String password)
	{
		try
		{
			// Create a new CyVerse account given the host address, port, username, password, homedirectory, and one field I have no idea what it does..., however leaving it as empty string makes file creation work!
			IRODSAccount account = IRODSAccount.instance(CYVERSE_HOST, 1247, username, password, HOME_DIRECTORY + username, ZONE, "", AuthScheme.STANDARD);
			// Create a new session
			IRODSSession session = IRODSSession.instance(IRODSSimpleProtocolManager.instance());
			// Create an irodsAO
			IRODSAccessObjectFactory irodsAO = IRODSAccessObjectFactoryImpl.instance(session);
			// Perform the authentication and get a response
			AuthResponse authResponse = irodsAO.authenticateIRODSAccount(account);
			// If the authentication worked, return true and set the username and logged in fields
			if (authResponse.isSuccessful())
			{
				// Cache the authenticated IRODS account
				this.authenticatedAccount = authResponse.getAuthenticatedIRODSAccount();

				// Store a session manager
				this.sessionManager = new CyVerseSessionManager(this.authenticatedAccount);

				// We're good, return true
				return true;
			}
			else
			{
				// If the authentication failed, print a message, and logout in case the login partially completed
				CalliopeData.getInstance().getErrorDisplay().printError("Authentication failed. Response was: " + authResponse.getAuthMessage());
			}
			session.closeSession(account);
		}
		// If the authentication failed, print a message, and logout in case the login partially completed
		catch (InvalidUserException | AuthenticationException e)
		{
			CalliopeData.getInstance().getErrorDisplay().printError("Authentication failed!");
		}
		// If the authentication failed due to a jargon exception, print a message, and logout in case the login partially completed
		// Not really sure how this happens, probably if the server incorrectly responds or is down
		catch (JargonException e)
		{
			CalliopeData.getInstance().getErrorDisplay().notify("Could not authenticate the user!\n" + ExceptionUtils.getStackTrace(e));
		}
		// Default, just return false
		return false;
	}

	/**
	 * Connects to CyVerse and uploads the given collection to CyVerse's data store
	 *
	 * @param collection The list of new species to upload
	 */
	public void pushLocalCollection(ImageCollection collection, StringProperty messageCallback)
	{
		if (this.sessionManager.openSession())
		{
			// Check if we are the owner of the collection
			String ownerUsername = collection.getOwner();
			if (ownerUsername != null && ownerUsername.equals(CalliopeData.getInstance().getUsername()))
			{
				try
				{
					IRODSFileFactory fileFactory = this.sessionManager.getCurrentAO().getIRODSFileFactory(this.authenticatedAccount);

					// The name of the collection directory is the UUID of the collection
					String collectionDirName = COLLECTIONS_DIRECTORY + "/" + collection.getID().toString();

					// Create the directory, and set the permissions appropriately
					IRODSFile collectionDir = fileFactory.instanceIRODSFile(collectionDirName);
					if (!collectionDir.exists())
						collectionDir.mkdir();
					this.setFilePermissions(collectionDirName, collection.getPermissions(), false);

					if (messageCallback != null)
						messageCallback.setValue("Writing collection Uploads directory...");

					// Create the folder containing uploads, and set its permissions
					IRODSFile collectionDirUploads = fileFactory.instanceIRODSFile(collectionDirName + "/Uploads");
					if (!collectionDirUploads.exists())
						collectionDirUploads.mkdir();
					this.setFilePermissions(collectionDirUploads.getAbsolutePath(), collection.getPermissions(), true);
				}
				catch (JargonException e)
				{
					CalliopeData.getInstance().getErrorDisplay().notify("Error creating the collections directory! Error was:\n" + ExceptionUtils.getStackTrace(e));
				}
			}

			this.sessionManager.closeSession();
		}
	}

	/**
	 * Removes a collection from CyVerse's system
	 *
	 * @param collection The collection to delete from CyVerse
	 */
	public void removeCollection(ImageCollection collection)
	{
		if (this.sessionManager.openSession())
		{
			// The name of the collection to remove
			String collectionsDirName = COLLECTIONS_DIRECTORY + "/" + collection.getID().toString();
			try
			{
				// If it exists, delete it
				IRODSFile collectionDir = this.sessionManager.getCurrentAO().getIRODSFileFactory(this.authenticatedAccount).instanceIRODSFile(collectionsDirName);
				if (collectionDir.exists())
					collectionDir.delete();
			}
			catch (JargonException e)
			{
				CalliopeData.getInstance().getErrorDisplay().notify("Could not delete the collection from CyVerse!\n" + ExceptionUtils.getStackTrace(e));
			}
			this.sessionManager.closeSession();
		}
	}

	/**
	 * Sets the file permission for a file on the CyVerse system
	 *
	 * @param fileName The name of the file to update permissions of
	 * @param permissions The list of permissions to set
	 * @param recursive If the permissions are to be recursive
	 * @throws JargonException Thrown if something goes wrong in the Jargon library
	 */
	private void setFilePermissions(String fileName, ObservableList<Permission> permissions, boolean recursive) throws JargonException
	{
		// Create the file, and remove all permissions from it
		IRODSFile file = this.sessionManager.getCurrentAO().getIRODSFileFactory(this.authenticatedAccount).instanceIRODSFile(fileName);
		this.removeAllFilePermissions(file);
		// If the file is a directory, set the directory permissions
		if (file.isDirectory())
		{
			// Go through each non-owner permission
			CollectionAO collectionAO = this.sessionManager.getCurrentAO().getCollectionAO(this.authenticatedAccount);
			permissions.filtered(permission -> !permission.isOwner()).forEach(permission -> {
				try
				{
					// If the user can upload, and we're not forcing read only, set the permission to write
					if (permission.canUpload())
						collectionAO.setAccessPermissionWrite(ZONE, file.getAbsolutePath(), permission.getUsername(), recursive);
					// If the user can read set the permission to write
					else if (permission.canRead())
						collectionAO.setAccessPermissionRead(ZONE, file.getAbsolutePath(), permission.getUsername(), recursive);
				}
				catch (JargonException e)
				{
					CalliopeData.getInstance().getErrorDisplay().notify("Error setting permissions for user!\n" + ExceptionUtils.getStackTrace(e));
				}
			});
		}
		// File permissions are done differently, so do that here
		else if (file.isFile())
		{
			DataObjectAO dataObjectAO = this.sessionManager.getCurrentAO().getDataObjectAO(this.authenticatedAccount);
			// Go through each permission and set the file permissions
			permissions.filtered(permission -> !permission.isOwner()).forEach(permission -> {
				try
				{
					// If the user can upload, and we're not forcing read only, set the permission to write
					if (permission.canUpload())
						dataObjectAO.setAccessPermissionWrite(ZONE, file.getAbsolutePath(), permission.getUsername());
						// If the user can read set the permission to write
					else if (permission.canRead())
						dataObjectAO.setAccessPermissionRead(ZONE, file.getAbsolutePath(), permission.getUsername());
				}
				catch (JargonException e)
				{
					CalliopeData.getInstance().getErrorDisplay().notify("Error setting permissions for user!\n" + ExceptionUtils.getStackTrace(e));
				}
			});
		}
	}

	/**
	 * Removes all file permissions except the owner
	 *
	 * @param file The file to remove permission from
	 * @throws JargonException Thrown if something goes wrong in the Jargon library
	 */
	private void removeAllFilePermissions(IRODSFile file) throws JargonException
	{
		// Directories are done differently than files, so test this first
		if (file.isDirectory())
		{
			// If it's a collection, we list all permission for the folder
			CollectionAndDataObjectListingEntry collectionPermissions = this.sessionManager.getCurrentAO().getCollectionAndDataObjectListAndSearchAO(this.authenticatedAccount).getCollectionAndDataObjectListingEntryAtGivenAbsolutePath(file.getAbsolutePath());
			CollectionAO collectionAO = this.sessionManager.getCurrentAO().getCollectionAO(this.authenticatedAccount);
			// We go through each permission, and remove all access permissions from that user
			collectionPermissions.getUserFilePermission().forEach(userFilePermission -> {
				if (userFilePermission.getFilePermissionEnum() != FilePermissionEnum.OWN)
					try
					{
						collectionAO.removeAccessPermissionForUser(ZONE, file.getAbsolutePath(), userFilePermission.getUserName(), true);
					}
					catch (JargonException e)
					{
						CalliopeData.getInstance().getErrorDisplay().notify("Error removing permissions from user!\n" + ExceptionUtils.getStackTrace(e));
					}
			});
		}
		else if (file.isFile())
		{
			// If it's a file, we list all permission for the file
			DataObjectAO dataObjectAO = this.sessionManager.getCurrentAO().getDataObjectAO(this.authenticatedAccount);
			// We go through each permission, and remove all access permissions from that user
			dataObjectAO.listPermissionsForDataObject(file.getAbsolutePath()).forEach(userFilePermission -> {
				if (userFilePermission.getFilePermissionEnum() != FilePermissionEnum.OWN)
					try
					{
						dataObjectAO.removeAccessPermissionsForUser(ZONE, file.getAbsolutePath(), userFilePermission.getUserName());
					}
					catch (JargonException e)
					{
						CalliopeData.getInstance().getErrorDisplay().notify("Error removing permissions from user!\n" + ExceptionUtils.getStackTrace(e));
					}
			});
		}
	}

	/**
	 * Test to see if the given username is valid on the CyVerse system
	 *
	 * @param username The username to test
	 * @return True if the username exists on CyVerse, false otherwise
	 */
	public Boolean isValidUsername(String username)
	{
		if (this.sessionManager.openSession())
		{
			try
			{
				User byName = this.sessionManager.getCurrentAO().getUserAO(this.authenticatedAccount).findByName(username);
				// Grab the user object for a given name, if it's null, it doesn't exist!
				this.sessionManager.closeSession();
				return byName != null;
			}
			catch (JargonException ignored)
			{
			}
			this.sessionManager.closeSession();
		}
		return false;
	}

	/**
	 * Uploads a set of images to CyVerse
	 *
	 * @param collection The collection to upload to
	 * @param directoryToWrite The directory to write
	 * @param transferCallback The callback that will receive callbacks if the transfer is in progress
	 * @param messageCallback Optional message callback that will show what is currently going on
	 */
	public void uploadImages(ImageCollection collection, ImageDirectory directoryToWrite, TransferStatusCallbackListener transferCallback, StringProperty messageCallback)
	{
		if (this.sessionManager.openSession())
		{
			try
			{
				// Grab the uploads folder for a given collection
				String collectionUploadDirStr = COLLECTIONS_DIRECTORY + "/" + collection.getID().toString() + "/Uploads";
				IRODSFileFactory fileFactory = this.sessionManager.getCurrentAO().getIRODSFileFactory(this.authenticatedAccount);
				IRODSFile collectionUploadDir = fileFactory.instanceIRODSFile(collectionUploadDirStr);
				// If the uploads directory exists and we can write to it, upload
				if (collectionUploadDir.exists() && collectionUploadDir.canWrite())
				{
					if (messageCallback != null)
						messageCallback.setValue("Creating upload folder on CyVerse...");

					// Create a new folder for the upload, we will use the current date as the name plus our username
					String uploadFolderName = FOLDER_FORMAT.format(new Date(this.sessionManager.getCurrentAO().getEnvironmentalInfoAO(this.authenticatedAccount).getIRODSServerCurrentTime())) + " " + CalliopeData.getInstance().getUsername();
					String uploadDirName = collectionUploadDirStr + "/" + uploadFolderName;

					if (messageCallback != null)
						messageCallback.setValue("Creating TAR file out of the directory before uploading...");

					// Create the JSON file representing the upload
					Integer imageCount = Math.toIntExact(directoryToWrite.flattened().filter(imageContainer -> imageContainer instanceof ImageEntry).count());
					UploadedEntry uploadEntry = new UploadedEntry(CalliopeData.getInstance().getUsername(), LocalDateTime.now(), imageCount, uploadDirName);

					// Create the meta.csv representing the metadata for all images in the tar file
					String localDirAbsolutePath = directoryToWrite.getFile().getAbsolutePath();
					String localDirName = directoryToWrite.getFile().getName();

					// Make a set of tar files from the image files. Don't use a single tar file because we may have > 1000 images in each
					File[] tarsToWrite = DirectoryManager.directoryToTars(directoryToWrite, imageEntry ->
					{
						// Compute the image's "cyverse" path
						String fileRelativePath = localDirName + StringUtils.substringAfter(imageEntry.getFile().getAbsolutePath(), localDirAbsolutePath);
						fileRelativePath = fileRelativePath.replace('\\', '/');
						return fileRelativePath + "\n";
					}, 900);

					// For each tar part, upload
					for (Integer tarPart = 0; tarPart < tarsToWrite.length; tarPart++)
					{
						if (messageCallback != null)
							messageCallback.setValue("Uploading TAR file part (" + (tarPart + 1) + " / " + tarsToWrite.length + ") to CyVerse...");

						File toWrite = tarsToWrite[tarPart];
						File localToUpload = new File(FilenameUtils.getFullPath(toWrite.getAbsolutePath()) + uploadFolderName + "-" + tarPart.toString() + "." + FilenameUtils.getExtension(toWrite.getAbsolutePath()));
						toWrite.renameTo(localToUpload);
						// Upload the tar
						this.sessionManager.getCurrentAO().getDataTransferOperations(this.authenticatedAccount).putOperation(localToUpload, collectionUploadDir, transferCallback, null);

						localToUpload.delete();
					}

					// Finally we actually index the image metadata using elasticsearch
					CalliopeData.getInstance().getEsConnectionManager().indexImages(uploadDirName + "/" + localDirName, collection.getID().toString(), directoryToWrite, uploadEntry);

					// Let rules do the rest!
				}
				else
				{
					CalliopeData.getInstance().getErrorDisplay().notify("You don't have permission to upload to this collection!");
				}
			}
			catch (JargonException e)
			{
				CalliopeData.getInstance().getErrorDisplay().notify("Could not upload the images to CyVerse!\n" + ExceptionUtils.getStackTrace(e));
			}
			this.sessionManager.closeSession();
		}
	}

	/**
	 * Downloads a CyVerse file to a local file
	 *
	 * @param cyverseFile The file in CyVerse to download
	 * @return The local file
	 */
	public File remoteToLocalImageFile(IRODSFile cyverseFile)
	{
		if (this.sessionManager.openSession())
		{
			try
			{
				// Grab the name of the CyVerse file
				String fileName = cyverseFile.getName();
				// Create a temporary file to write to with the same name
				File localImageFile = CalliopeData.getInstance().getTempDirectoryManager().createTempFile(fileName);

				// Download the file locally
				this.sessionManager.getCurrentAO().getDataTransferOperations(this.authenticatedAccount).getOperation(cyverseFile, localImageFile, new TransferStatusCallbackListener()
				{
					@Override
					public FileStatusCallbackResponse statusCallback(TransferStatus transferStatus) { return FileStatusCallbackResponse.CONTINUE; }
					@Override
					public void overallStatusCallback(TransferStatus transferStatus) {}
					@Override
					public CallbackResponse transferAsksWhetherToForceOperation(String irodsAbsolutePath, boolean isCollection) { return CallbackResponse.YES_FOR_ALL; }
				}, null);

				this.sessionManager.closeSession();
				return localImageFile;
			}
			catch (JargonException e)
			{
				CalliopeData.getInstance().getErrorDisplay().notify("Could not pull the remote file (" + cyverseFile.getName() + ")!\n" + ExceptionUtils.getStackTrace(e));
			}
			this.sessionManager.closeSession();
		}

		return null;
	}

	/**
	 * Called to download the folder as an image directory from a CyVerse absolute path
	 *
	 * @param absolutePathToFiles The absolute path to the files to index
	 * @return An image directory representing the CyVerse datastore absolute path
	 */
	public ImageDirectory prepareExistingImagesForIndexing(String absolutePathToFiles)
	{
		// Open a session as usual
		if (this.sessionManager.openSession())
		{
			try
			{
				// Create a file factory object
				IRODSFileFactory irodsFileFactory = this.sessionManager.getCurrentAO().getIRODSFileFactory(this.authenticatedAccount);
				// Create an instance of an iRODS file using the file factory
				IRODSFileImpl topLevelDirectory = (IRODSFileImpl) irodsFileFactory.instanceIRODSFile(absolutePathToFiles);
				// Make sure that the directory exists, and can be read, and is a directory
				if (topLevelDirectory.exists() && topLevelDirectory.canRead() && topLevelDirectory.isDirectory())
				{
					// Create a new CyVerse datastore image directory representing the image
					CyVerseDSImageDirectory imageDirectory = new CyVerseDSImageDirectory(topLevelDirectory);
					// Download the image directory for editing using a recursive call
					this.createDirectoryAndImageTree(imageDirectory);
					// Return the directory
					return imageDirectory;
				}
			}
			catch (JargonException e)
			{
				// If something goes wrong, display an error
				CalliopeData.getInstance().getErrorDisplay().notify("Could not download existing image data for indexing!\n" + ExceptionUtils.getStackTrace(e));
			}

			// Close the session
			this.sessionManager.closeSession();
		}
		return null;
	}

	/**
	 * Recursively adds all sub-directories and images to a directory
	 *
	 * @param currentDirectory The directory to recursively add sub-directories and images to
	 */
	private void createDirectoryAndImageTree(CyVerseDSImageDirectory currentDirectory)
	{
		// Get all directory sub-files
		IRODSFileImpl[] subFiles = (IRODSFileImpl[]) currentDirectory.getCyverseFile().listFiles();

		// Make sure it's not null
		if (subFiles != null)
		{
			// Iterate over all sub-files
			for (IRODSFileImpl file : subFiles)
			{
				// If the file is not a directory add it as a new image entry
				if (!file.isDirectory())
					if (CalliopeAnalysisUtils.fileIsImage(file))
						currentDirectory.addImage(new CyVerseDSImageEntry(file));
				else
				{
					// Grab the sub-directory
					CyVerseDSImageDirectory subDirectory = new CyVerseDSImageDirectory(file);
					// Store the sub-directory
					currentDirectory.addChild(subDirectory);
					// Recursively read the sub-directory
					this.createDirectoryAndImageTree(subDirectory);
				}
			}
		}
	}

	/**
	 * Reads the image from an iRODS absolute path into local memory
	 *
	 * @param irodsFileAbsolutePath The iRODS path to read from
	 * @return The image to return
	 */
	public BufferedImage readIRODSImage(String irodsFileAbsolutePath)
	{
		// Open the session as usual
		if (this.sessionManager.openSession())
		{
			try
			{
				// We need to duplicate the iRODS file. The IRODSInputStream does not correctly close the file if an exception occurs so we need to take
				// care of that manually by creating a new iRODS file every time this method is called
				IRODSFileFactory fileFactory = this.sessionManager.getCurrentAO().getIRODSFileFactory(this.authenticatedAccount);
				IRODSFile irodsFile = fileFactory.instanceIRODSFile(irodsFileAbsolutePath);
				// Create an inputstream to the file
				try (InputStream fileInputStream = fileFactory.instanceIRODSFileInputStream(irodsFile))
				{
					// Read the stream as an image file
					return ImageIO.read(fileInputStream);
				}
			}
			catch (JargonException | IOException ignored)
			{
				// Ignore loading errors, just display a blank image then
			}
			finally
			{
				// Close the session
				this.sessionManager.closeSession();
			}
		}
		return null;
	}
}
