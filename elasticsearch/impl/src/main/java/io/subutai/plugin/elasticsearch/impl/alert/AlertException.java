package io.subutai.plugin.elasticsearch.impl.alert;


/**
 * Exception thrown on alert processing
 */
public class AlertException extends Exception
{
    public AlertException( final String message, final Throwable cause )
    {
        super( message, cause );
    }
}
