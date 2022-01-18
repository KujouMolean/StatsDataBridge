package com.molean.statsdatabridge;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;

public class PlayerDataDao {

    public static void checkTable() throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    create table if not exists playerdata
                    (
                        id       int primary key auto_increment,
                        uuid     varchar(100) not null unique,
                        data     longblob     not null,
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
                    from minecraft.playerdata
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

    public static void insert(UUID owner, byte[] data) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    insert into minecraft.playerdata(uuid, data)
                    values (?, ?);
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            preparedStatement.setBlob(2, byteArrayInputStream);
            preparedStatement.execute();
        }
    }

    public static boolean update(UUID owner, byte[] data, String passwd) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.playerdata
                    set data=?
                    where uuid = ? and passwd = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            preparedStatement.setBlob(1, byteArrayInputStream);
            preparedStatement.setString(2, owner.toString());
            preparedStatement.setString(3, passwd);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public static boolean complete(UUID owner, byte[] data, String passwd) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.playerdata
                    set data=? , passwd=null
                    where uuid = ? and passwd = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
            preparedStatement.setBlob(1, byteArrayInputStream);
            preparedStatement.setString(2, owner.toString());
            preparedStatement.setString(3, passwd);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public static @Nullable String getLock(UUID owner) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    update minecraft.playerdata
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
                    update minecraft.playerdata
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

    public static byte[] query(UUID owner) throws SQLException, IOException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    select data
                    from minecraft.playerdata
                    where uuid = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Blob blob = resultSet.getBlob(1);
                return blob.getBinaryStream().readAllBytes();
            }
        }
        throw new RuntimeException("Player data not exist, check exist before query!");
    }

    public static byte[] query(UUID owner, String passwd) throws SQLException, IOException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    select data
                    from minecraft.playerdata
                    where uuid = ? and passwd=?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, owner.toString());
            preparedStatement.setString(2, passwd);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Blob blob = resultSet.getBlob(1);
                return blob.getBinaryStream().readAllBytes();
            }
        }
        throw new RuntimeException("Player data not exist, check exist before query!");
    }

    public static void delete(UUID owner) throws SQLException {
        try (Connection connection = DataSourceUtils.getConnection()) {
            String sql = """
                    delete from minecraft.playerdata
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
                    update minecraft.playerdata set uuid=?
                    where uuid = ?;
                    """;
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, target.toString());
            preparedStatement.setString(2, source.toString());
            preparedStatement.executeUpdate();
        }
    }
}
