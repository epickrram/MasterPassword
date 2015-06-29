package com.lyndir.masterpassword.model;

import com.google.common.base.*;
import com.google.common.collect.*;
import com.google.common.io.CharSink;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;


/**
 * @author lhunath, 14-12-07
 */
public class MPUserFileManager extends MPUserManager {

    private static final Logger logger = Logger.getLogger(MPUserFileManager.class.getSimpleName());
    private static final File   mpwd   = new File( System.getProperty( "user.home" ), ".mpw.d" );
    private static final MPUserFileManager instance;

    static {
        File mpwrc = new File( System.getProperty( "user.home" ), ".mpwrc" );
        if (mpwrc.exists() && !mpwd.exists())
            if (!mpwrc.renameTo( mpwd ))
                logger.log(Level.SEVERE, String.format("Couldn't migrate: %s -> %s", mpwrc, mpwd ));

        instance = create( mpwd );
    }

    private final File userFilesDirectory;

    public static MPUserFileManager get() {
        MPUserManager.instance = instance;
        return instance;
    }

    public static MPUserFileManager create(final File userFilesDirectory) {
        return new MPUserFileManager( userFilesDirectory );
    }

    protected MPUserFileManager(final File userFilesDirectory) {

        super( unmarshallUsers( userFilesDirectory ) );
        this.userFilesDirectory = userFilesDirectory;
    }

    private static Iterable<MPUser> unmarshallUsers(final File userFilesDirectory) {
        if (!userFilesDirectory.mkdirs() && !userFilesDirectory.isDirectory()) {
            logger.severe( String.format("Couldn't create directory for user files: %s", userFilesDirectory ));
            return ImmutableList.of();
        }

        return FluentIterable.from( ImmutableList.copyOf( userFilesDirectory.listFiles( new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith( ".mpsites" );
            }
        } ) ) ).transform( new Function<File, MPUser>() {
            @Nullable
            @Override
            public MPUser apply(@Nullable final File file) {
                try {
                    return MPSiteUnmarshaller.unmarshall( Preconditions.checkNotNull( file ) ).getUser();
                }
                catch (IOException e) {
                    logger.log(Level.SEVERE, String.format("Couldn't read user from: %s", file), e );
                    return null;
                }
            }
        } ).filter( Predicates.notNull() );
    }

    @Override
    public void addUser(final MPUser user) {
        super.addUser( user );
        save();
    }

    @Override
    public void deleteUser(final MPUser user) {
        super.deleteUser( user );
        save();
    }

    /**
     * Write the current user state to disk.
     */
    public void save() {
        // Save existing users.
        for (final MPUser user : getUsers())
            try {
                new CharSink() {
                    @Override
                    public Writer openStream()
                            throws IOException {
                        return new FileWriter( new File( userFilesDirectory, user.getFullName() + ".mpsites" ) );
                    }
                }.write( MPSiteMarshaller.marshallSafe( user ).getExport() );
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, String.format("Unable to save sites for user: %s", user), e );
            }

        // Remove deleted users.
        for (File userFile : userFilesDirectory.listFiles( new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith( ".mpsites" );
            }
        } ))
            if (getUserNamed( userFile.getName().replaceFirst( "\\.mpsites$", "" ) ) == null)
                if (!userFile.delete())
                    logger.severe( String.format("Couldn't delete file: %s", userFile ));
    }

    /**
     * @return The location on the file system where the user models are stored.
     */
    public File getPath() {
        return mpwd;
    }
}
