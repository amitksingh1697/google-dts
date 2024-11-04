package com.google.cloud.connector.gcp;

import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpServerCodec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** A mock GCP Metadata server that returns result based on a static file. */
public class MockMetadataServer
    implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

  private Channel channel;
  private String httpProxyHost;
  private String httpProxyPort;

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    beforeEach(context);
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    ServerBootstrap bootstrap =
        new ServerBootstrap()
            .group(new NioEventLoopGroup(), new NioEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .childHandler(
                new ChannelInitializer<SocketChannel>() {
                  @Override
                  protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new RequestHandler());
                  }
                });
    channel = bootstrap.bind(0).sync().channel();

    httpProxyHost = System.getProperty("http.proxyHost");
    httpProxyPort = System.getProperty("http.proxyPort");
    System.setProperty("http.proxyHost", getBindAddress().getHostName());
    System.setProperty("http.proxyPort", String.valueOf(getBindAddress().getPort()));
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    afterEach(context);
  }

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    if (channel != null) {
      channel.close().sync();
    }
    Optional.ofNullable(httpProxyHost)
        .ifPresentOrElse(
            s -> System.setProperty("http.proxyHost", s),
            () -> System.clearProperty("http.proxyHost"));
    Optional.ofNullable(httpProxyPort)
        .ifPresentOrElse(
            s -> System.setProperty("http.proxyPort", s),
            () -> System.clearProperty("http.proxyPort"));
  }

  private InetSocketAddress getBindAddress() {
    return (InetSocketAddress) channel.localAddress();
  }

  /** A Http handler that response to Metadata server query based on a static json file. */
  private static final class RequestHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final String METADATA_RESPONSE_JSON = "metadata-response.json";
    private final Struct responseStruct;

    private RequestHandler() {
      URL responseFile = getClass().getClassLoader().getResource(METADATA_RESPONSE_JSON);
      if (responseFile == null) {
        throw new IllegalStateException("Missing response file " + METADATA_RESPONSE_JSON);
      }

      try (Reader r = new InputStreamReader(responseFile.openStream(), StandardCharsets.UTF_8)) {
        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().merge(r, builder);
        this.responseStruct = builder.build();
      } catch (IOException e) {
        throw new IllegalStateException("Failed to parse response file", e);
      }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) {
      URI uri = URI.create(msg.uri());
      HttpHeaders httpHeaders = new DefaultHttpHeaders().add("Metadata-Flavor", "Google");

      if (!Objects.equals(msg.headers().get("Metadata-Flavor"), "Google")) {
        ctx.channel().writeAndFlush(new DefaultHttpResponse(HTTP_1_1, FORBIDDEN, httpHeaders));
        return;
      }

      if (!responseStruct.containsFields(uri.getPath())) {
        ctx.channel().writeAndFlush(new DefaultHttpResponse(HTTP_1_1, NOT_FOUND, httpHeaders));
        return;
      }

      String body = responseStruct.getFieldsOrThrow(uri.getPath()).getStringValue();

      ctx.channel()
          .writeAndFlush(
              new DefaultFullHttpResponse(
                  HTTP_1_1,
                  OK,
                  Unpooled.wrappedBuffer(body.getBytes(StandardCharsets.UTF_8)),
                  httpHeaders,
                  new DefaultHttpHeaders()))
          .addListener(ChannelFutureListener.CLOSE);
    }
  }
}
