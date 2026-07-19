// package example.data.service;

// import org.springframework.boot.web.servlet.FilterRegistrationBean;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class FilterConfig {

//     @Bean
//     public FilterRegistrationBean<InternalGatewayFilter> gatewayFilter(
//             InternalGatewayFilter filter) {

//         FilterRegistrationBean<InternalGatewayFilter> bean = new FilterRegistrationBean<>();

//         bean.setFilter(filter);

//         bean.addUrlPatterns("/api/*");

//         bean.setOrder(1);

//         return bean;
//     }
// }