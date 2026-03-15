package com.sba.ssos.constant;

public final class ApiPaths {

  public static final String API_V1 = "/api/v1";
  public static final String ADMIN = API_V1 + "/admin";
  public static final String WEBHOOKS = API_V1 + "/webhooks";

  public static final String BRANDS = API_V1 + "/brands";
  public static final String CART = API_V1 + "/cart";
  public static final String CATEGORIES = API_V1 + "/categories";
  public static final String NOTIFICATIONS = API_V1 + "/notifications";
  public static final String STORAGE = API_V1 + "/storage";
  public static final String WISHLIST = API_V1 + "/wishlist";
  public static final String SHOES = API_V1 + "/shoes";
  public static final String REVIEWS = API_V1 + "/reviews";
  public static final String CUSTOMER_ORDERS = API_V1 + "/orders";
  public static final String USER_PROFILE = API_V1 + "/users/me";
  public static final String SEPAY_AUTH = API_V1 + "/sepay/auth";
  public static final String KEYCLOAK_WEBHOOKS = WEBHOOKS + "/keycloak";

  public static final String ADMIN_CHAT_LOGS = ADMIN + "/chat-logs";
  public static final String ADMIN_CHECKS = ADMIN + "/checks";
  public static final String ADMIN_DASHBOARD = ADMIN + "/dashboard";
  public static final String ADMIN_NOTIFICATIONS = ADMIN + "/notifications";
  public static final String ADMIN_ORDERS = ADMIN + "/orders";
  public static final String ADMIN_USERS = ADMIN + "/users";
  public static final String ADMIN_VECTOR_STORE = ADMIN + "/vector-store";

  private ApiPaths() {}
}
