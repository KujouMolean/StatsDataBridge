package com.molean.statsdatabridge;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsDao {
    public static void checkTable() throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                     create table if not exists player_stats
                    (
                        id       int primary key auto_increment,
                        uuid     varchar(100) not null unique,
                        stats    longtext     not null,
                        passwd   varchar(100) null default null
                    );""";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();

        }
    }

    public static boolean exist(UUID owner) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    select uuid
                    from minecraft.player_stats
                    where uuid = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

    public static void insert(UUID owner, String stats) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    insert into minecraft.player_stats(uuid, stats)
                    values ( ?,?);
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            preparedStatement.setString(2, stats);
            preparedStatement.execute();
        }
    }

    public static boolean update(UUID owner, String stats, String passwd) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.player_stats
                    set stats=?
                    where uuid = ? and passwd = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, stats);
            preparedStatement.setString(2, owner.toString());
            preparedStatement.setString(3, passwd);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public static boolean complete(UUID owner, String stats, String passwd) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.player_stats
                    set stats=? , passwd=null
                    where uuid = ? and passwd = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, stats);
            preparedStatement.setString(2, owner.toString());
            preparedStatement.setString(3, passwd);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public static @Nullable String getLock(UUID owner) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.player_stats
                    set passwd = ?
                    where uuid = ? and passwd is null;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            String passwd = UUID.randomUUID().toString();
            preparedStatement.setString(1, passwd);
            preparedStatement.setString(2, owner.toString());
            int i = preparedStatement.executeUpdate();
            if (i > 0) {
                return passwd;
            } else {
                return null;
            }
        }
    }

    public static String getLockForce(UUID owner) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.player_stats
                    set passwd = ?
                    where uuid = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            String passwd = UUID.randomUUID().toString();
            preparedStatement.setString(1, passwd);
            preparedStatement.setString(2, owner.toString());
            int i = preparedStatement.executeUpdate();
            if (i > 0) {
                return passwd;
            } else {
                return null;
            }
        }
    }

    public static Map<UUID, String> queryAll() throws SQLException {
        Map<UUID,String> map = new HashMap<>();
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    select uuid,stats
                    from minecraft.player_stats
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString(1));
                String stats = resultSet.getString(2);
                map.put(uuid, stats);
            }
        }
        return map;
    }
    public static String queryForce(UUID owner) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    select stats
                    from minecraft.player_stats
                    where uuid = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        throw new RuntimeException("Player stats not exist, check exist before query!");
    }

    public static String query(UUID owner, String passwd) throws SQLException, IOException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    select stats
                    from minecraft.player_stats
                    where uuid = ? and passwd=?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            preparedStatement.setString(2, passwd);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        throw new RuntimeException("Player stats not exist, check exist before query!");
    }

    public static void delete(UUID owner) throws SQLException, IOException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    delete from minecraft.player_stats
                    where uuid = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            preparedStatement.executeUpdate();
        }
    }

    public static void replace(UUID source, UUID target) throws SQLException, IOException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.player_stats set uuid=?
                    where uuid = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, target.toString());
            preparedStatement.setString(2, source.toString());
            preparedStatement.executeUpdate();
        }
    }
}
