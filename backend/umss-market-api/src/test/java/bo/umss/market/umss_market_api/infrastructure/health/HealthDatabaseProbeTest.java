package bo.umss.market.umss_market_api.infrastructure.health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

class HealthDatabaseProbeTest {
    @Test
    void handlesConnectionFailure() throws Exception {
        DataSource source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("Connection unavailable"));
        assertFalse(new HealthDatabaseProbe(source).isUp());
    }

    @Test
    void handlesQueryTimeoutAndReleasesConnection() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenThrow(new SQLTimeoutException());
        assertFalse(new HealthDatabaseProbe(source).isUp());
        verify(statement).setQueryTimeout(5);
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void rejectsUnexpectedQueryResultAndReleasesResources() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        java.sql.ResultSetMetaData metadata = mock(java.sql.ResultSetMetaData.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(result);
        when(result.getMetaData()).thenReturn(metadata);
        when(metadata.getColumnCount()).thenReturn(1);
        when(result.next()).thenReturn(true, false);
        when(result.getInt(1)).thenReturn(0);
        assertFalse(new HealthDatabaseProbe(source).isUp());
        verify(result).close();
        verify(statement).close();
        verify(connection).close();
    }
}
