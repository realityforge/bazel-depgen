package org.realityforge.bazel.depgen;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;

final class SimpleTransferListener
  extends AbstractTransferListener
{
  @Nonnull
  private final Map<TransferResource, Long> _downloads = new ConcurrentHashMap<>();
  @Nonnull
  private final Logger _logger;
  private int lastLength;

  SimpleTransferListener( @Nonnull final Logger logger )
  {
    _logger = Objects.requireNonNull( logger );
  }

  @Override
  public void transferInitiated( @Nonnull final TransferEvent event )
  {
    if ( _logger.isLoggable( Level.FINE ) )
    {
      _logger.fine( ( TransferEvent.RequestType.PUT == event.getRequestType() ? "Uploading" : "Downloading" ) +
                    ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName() );
    }
  }

  @Override
  public void transferProgressed( @Nonnull final TransferEvent event )
  {
    if ( _logger.isLoggable( Level.FINE ) )
    {
      final TransferResource resource = event.getResource();
      _downloads.put( resource, event.getTransferredBytes() );

      final StringBuilder buffer = new StringBuilder( 64 );

      for ( final Map.Entry<TransferResource, Long> entry : _downloads.entrySet() )
      {
        final long total = entry.getKey().getContentLength();
        final long complete = entry.getValue();

        buffer.append( getStatus( complete, total ) ).append( "  " );
      }

      final int pad = lastLength - buffer.length();
      lastLength = buffer.length();
      pad( buffer, pad );
      buffer.append( '\r' );

      _logger.fine( buffer.toString() );
    }
  }

  @Override
  public void transferSucceeded( @Nonnull final TransferEvent event )
  {
    transferCompleted( event );

    if ( _logger.isLoggable( Level.FINE ) )
    {
      final TransferResource resource = event.getResource();
      final long contentLength = event.getTransferredBytes();
      if ( contentLength >= 0 )
      {
        final String len = contentLength >= 1024 ? toKB( contentLength ) + " KB" : contentLength + " B";

        final long duration = System.currentTimeMillis() - resource.getTransferStartTime();
        final String throughput;
        if ( duration > 0 )
        {
          final long bytes = contentLength - resource.getResumeOffset();
          final DecimalFormat format = new DecimalFormat( "0.0", new DecimalFormatSymbols( Locale.ENGLISH ) );
          final double kbPerSec = ( bytes / 1024.0 ) / ( duration / 1000.0 );
          throughput = " at " + format.format( kbPerSec ) + " KB/sec";
        }
        else
        {
          throughput = "";
        }

        _logger.fine( ( TransferEvent.RequestType.PUT == event.getRequestType() ? "Uploaded" : "Downloaded" ) + ": " +
                      resource.getRepositoryUrl() + resource.getResourceName() +
                      " (" + len + throughput + ")" );
      }
    }
  }

  @Override
  public void transferFailed( @Nonnull final TransferEvent event )
  {
    transferCompleted( event );

    if ( !( event.getException() instanceof MetadataNotFoundException ) )
    {
      if ( _logger.isLoggable( Level.INFO ) )
      {
        _logger.log( Level.INFO,
                     "Transfer Failed: " + event.getResource().getResourceName(),
                     event.getException() );
      }
    }
  }

  private void transferCompleted( @Nonnull final TransferEvent event )
  {
    _downloads.remove( event.getResource() );

    if ( _logger.isLoggable( Level.FINE ) )
    {
      final StringBuilder buffer = new StringBuilder( 64 );
      pad( buffer, lastLength );
      buffer.append( '\r' );
    }
  }

  @Override
  public void transferCorrupted( @Nonnull final TransferEvent event )
  {
    if ( _logger.isLoggable( Level.WARNING ) )
    {
      _logger.log( Level.WARNING,
                   "Transfer Corrupted: " + event.getResource().getResourceName(),
                   event.getException() );
    }
  }

  @Nonnull
  private String getStatus( final long complete, final long total )
  {
    if ( total >= 1024 )
    {
      return toKB( complete ) + "/" + toKB( total ) + " KB ";
    }
    else if ( total >= 0 )
    {
      return complete + "/" + total + " B ";
    }
    else if ( complete >= 1024 )
    {
      return toKB( complete ) + " KB ";
    }
    else
    {
      return complete + " B ";
    }
  }

  private long toKB( final long bytes )
  {
    return ( bytes + 1023 ) / 1024;
  }

  private void pad( @Nonnull final StringBuilder buffer, final int spaces )
  {
    for ( int i = 0; i < spaces; i++ )
    {
      buffer.append( ' ' );
    }
  }
}
