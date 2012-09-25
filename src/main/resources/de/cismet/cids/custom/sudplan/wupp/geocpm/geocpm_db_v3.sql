DROP VIEW  IF EXISTS geocpm_breaking_edge_layer;

DROP TABLE IF EXISTS geocpm_jt_manhole_triangle;
DROP TABLE IF EXISTS geocpm_manhole;
DROP TABLE IF EXISTS geocpm_jt_breaking_edge_triangle;
DROP TABLE IF EXISTS geocpm_breaking_edge;
DROP TABLE IF EXISTS geocpm_source_drain;
DROP TABLE IF EXISTS geocpm_curve_value;
DROP TABLE IF EXISTS geocpm_curve;
DROP TABLE IF EXISTS geocpm_aus_info;
DROP TABLE IF EXISTS geocpm_aus_max;
DROP SEQUENCE IF EXISTS geocpm_breaking_edge_seq;
DROP INDEX    IF EXISTS geocpm_triangle_index_idx;
DROP TABLE IF EXISTS geocpm_triangle;
DROP INDEX    IF EXISTS geocpm_point_index_idx;
DROP TABLE IF EXISTS geocpm_point;
DROP TABLE IF EXISTS geocpm_configuration;
DROP SEQUENCE IF EXISTS geocpm_configuration_seq;
DROP TABLE IF EXISTS delta_configuration;
DROP TABLE IF EXISTS delta_breaking_edge;
DROP TABLE IF EXISTS delta_configuration_delta_breaking_edge;
DROP SEQUENCE IF EXISTS delta_breaking_edge_seq;
DROP SEQUENCE IF EXISTS delta_configuration_delta_breaking_edge_seq;
DROP SEQUENCE IF EXISTS delta_configuration_seq;
DROP INDEX IF EXISTS geocpm_breaking_edge_index_idx;
DROP INDEX IF EXISTS geocpm_manhole_index_idx;
DROP TABLE    IF EXISTS rainevent;
DROP SEQUENCE IF EXISTS rainevent_seq;


CREATE SEQUENCE geocpm_configuration_seq MINVALUE 1 START 1;

-- we use an integer pkey so that there won't be an issue with cids
-- we don't use serial because of cids, too
CREATE TABLE geocpm_configuration
(
  id integer NOT NULL DEFAULT nextval('geocpm_configuration_seq'::regclass),
  calc_begin timestamp without time zone,
  calc_end timestamp without time zone,
  write_node boolean,
  write_edge boolean,
  last_values boolean,
  save_marked boolean,
  merge_triangles boolean,
  min_calc_triangle_size numeric(20,8),
  time_step_restriction boolean,
  save_velocity_curves boolean,
  save_flow_curves boolean,
  result_save_limit numeric(20,8),
  number_of_threads integer,
  q_in integer,
  q_out integer,
  geom integer,
  dyna_form text,
  geocpmi_d text,
  geocpmf_d text,
  geocpms_d text,
  geocpm_ein_folder character varying(50),
  dyna_ein_folder character varying(50),
  geocpmn_d text,
  name character varying(200),
  description text,
  investigation_area integer,
  CONSTRAINT geocpm_configuration_pkey PRIMARY KEY (id ),
  CONSTRAINT geocpm_configuration_geom_fkey FOREIGN KEY (geom)
      REFERENCES geom (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
);


-- we use the geom table of cids

CREATE TABLE geocpm_point (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);
SELECT AddGeometryColumn('public', 'geocpm_point', 'geom', 31466, 'POINT', 3);
CREATE INDEX geocpm_point_index_idx ON geocpm_point (geocpm_configuration_id, index);

CREATE TABLE geocpm_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,
    geocpm_point_a_id BIGINT,
    geocpm_point_b_id BIGINT,
    geocpm_point_c_id BIGINT,
    neighbour_a_id INTEGER,
    neighbour_b_id INTEGER,
    neighbour_c_id INTEGER,
    roughness NUMERIC(18, 3),
    loss NUMERIC(18, 3),
    be_height_a NUMERIC(18, 3),
    be_height_b NUMERIC(18, 3),
    be_height_c NUMERIC(18, 3),
    marked BOOLEAN,
    tmp_point_a_id INTEGER,
    tmp_point_b_id INTEGER,
    tmp_point_c_id INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geocpm_point_a_id) REFERENCES geocpm_point,
    FOREIGN KEY (geocpm_point_b_id) REFERENCES geocpm_point,
    FOREIGN KEY (geocpm_point_c_id) REFERENCES geocpm_point
);
SELECT AddGeometryColumn('public', 'geocpm_triangle', 'geom', 31466, 'POLYGON', 3);

CREATE INDEX geocpm_triangle_index_idx ON geocpm_triangle  (geocpm_configuration_id, index);

-- we use an integer pkey so that there won't be an issue with cids
-- we don't use serial because of cids, too
CREATE TABLE geocpm_breaking_edge (
    id INTEGER PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    index INTEGER,
    type INTEGER,
    height numeric(14, 2),
    triangle_count_high INTEGER,
    triangle_count_low INTEGER,
    geom INTEGER,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geom) REFERENCES geom
);
-- we use the geom table of cids
--SELECT AddGeometryColumn('public', 'geocpm_breaking_edge', 'geom', 31466, 'LINESTRING', 2);
CREATE INDEX geocpm_breaking_edge_index_idx ON geocpm_breaking_edge  (geocpm_configuration_id, index);


-- this is for cids
CREATE SEQUENCE geocpm_breaking_edge_seq MINVALUE 1 START 1;
ALTER TABLE geocpm_breaking_edge ALTER COLUMN id SET DEFAULT nextval('geocpm_breaking_edge_seq');

--CREATE SEQUENCE geocpm_breaking_edge_proxy_seq MINVALUE 1 START 1;
---- Creates the new table "geocpm_breaking_edge_proxy" with the columns "height NUMERIC NULL, geocpm_configuration_proxy INTEGER NULL, id INTEGER PRIMARY KEY DEFAULT nextval('geocpm_breaking_edge_proxy_seq')".
--CREATE TABLE geocpm_breaking_edge_proxy(
--    id INTEGER PRIMARY KEY DEFAULT nextval('geocpm_breaking_edge_proxy_seq'),
--    geocpm_configuration_proxy INTEGER, 
--    height NUMERIC(14,2),
--
--    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
--);


CREATE TABLE geocpm_jt_breaking_edge_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_breaking_edge_id INTEGER,
    geocpm_triangle_id BIGINT,
    orientation char(1),

    FOREIGN KEY (geocpm_breaking_edge_id) REFERENCES geocpm_breaking_edge,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle
);


CREATE TABLE geocpm_curve (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    identifier VARCHAR(200),

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);

CREATE TABLE geocpm_curve_value (
    id BIGSERIAL PRIMARY KEY,
    geocpm_curve_id BIGINT,
    t NUMERIC(18, 3),
    value NUMERIC(18, 3),

    FOREIGN KEY (geocpm_curve_id) REFERENCES geocpm_curve
);

CREATE TABLE geocpm_source_drain (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    identifier VARCHAR(200),
    geocpm_triangle_id BIGINT,
    max_capacity NUMERIC(18, 3),
    geocpm_curve_id BIGINT,

    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle,
    FOREIGN KEY (geocpm_curve_id) REFERENCES geocpm_curve
);

CREATE TABLE geocpm_manhole (
    id BIGSERIAL PRIMARY KEY,
    geocpm_configuration_id INTEGER,
    internal_id INTEGER,
    cap_height NUMERIC(17, 2),
    free_leakage INTEGER,
    entry_profile NUMERIC(17, 2),
    loss_overfall NUMERIC(17, 2),
    loss_emersion NUMERIC(17, 2),
    length_emersion NUMERIC(17, 2),
    name VARCHAR(200),
    FOREIGN KEY (geocpm_configuration_id) REFERENCES geocpm_configuration
);

CREATE INDEX geocpm_manhole_index_idx ON geocpm_manhole  (geocpm_configuration_id, name);

CREATE TABLE geocpm_jt_manhole_triangle (
    id BIGSERIAL PRIMARY KEY,
    geocpm_manhole_id BIGINT,
    geocpm_triangle_id BIGINT,

    FOREIGN KEY (geocpm_manhole_id) REFERENCES geocpm_manhole,
    FOREIGN KEY (geocpm_triangle_id) REFERENCES geocpm_triangle
);


CREATE VIEW geocpm_breaking_edge_layer AS 
    SELECT    be.height, be.type, st_transform(st_setsrid(g.geo_field, 4326), 31466) AS geo_field
    FROM      geocpm_breaking_edge be
    LEFT JOIN geom g ON g.id = be.geom;

-------------------------------------------------------------------------
------------- GeoCPM Model Output Import ---------------------------------------
-------------------------------------------------------------------------

CREATE TABLE geocpm_aus_info
(

  geocpm_configuration_id     BIGINT,
  delta_configuration_id BIGINT,
  number_of_elements   BIGINT,                                     
  number_of_edges      BIGINT,                                          
  number_of_calc_steps BIGINT,                                           
  volume_drain_source           NUMERIC(10, 2),
  volume_street                 NUMERIC(10, 2), 
  volume_all                    NUMERIC(10, 2), 
  volume_loss                   NUMERIC(10, 2), 
  volume_exchange_dyna_geocpm   NUMERIC(10, 2), 
  volume_exchange_geocpm_dyna   NUMERIC(10, 2), 
  rain_surface_elements         NUMERIC(10, 2), 
  time_total                             NUMERIC(10, 2), 
  time_time_step_calc                    NUMERIC(10, 2),
  time_boundary_conditions               NUMERIC(10, 2), 
  time_boundary_conditions_source_drain  NUMERIC(10, 2), 
  time_boundary_conditions_manhole       NUMERIC(10, 2), 
  time_boundary_conditions_triangle      NUMERIC(10, 2),
  time_dgl                               NUMERIC(10, 2),
  time_overhead                          NUMERIC(10, 2)

  PRIMARY KEY (geocpm_configuration_id, delta_configuration_id)
);


CREATE TABLE geocpm_aus_max
(
  geocpm_configuration_id BIGINT,     
  delta_configuration_id  BIGINT,  
  geocpm_triangle_id      BIGINT,          
  water_level             NUMERIC(20, 10),

  PRIMARY KEY (geocpm_configuration_id, delta_configuration_id, geocpm_triangle_id)
);


----------------------------------------------------------------
------- DELTA
----------------------------------------------------------------


CREATE SEQUENCE delta_breaking_edge_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 11
  CACHE 1;

CREATE SEQUENCE delta_configuration_delta_breaking_edge_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 11
  CACHE 1;


CREATE SEQUENCE delta_configuration_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 4
  CACHE 1;


CREATE TABLE delta_configuration
(
  description text,
  locked boolean DEFAULT false,
  name character varying(200),
  id integer NOT NULL DEFAULT nextval('delta_configuration_seq'::regclass),
  delta_breaking_edges integer,
  original_object integer,
  CONSTRAINT delta_configuration_pkey PRIMARY KEY (id )
);

CREATE TABLE delta_breaking_edge
(
  name character varying(200),
  height numeric(14,2),
  description text,
  id integer NOT NULL DEFAULT nextval('delta_breaking_edge_seq'::regclass),
  original_object integer,
  CONSTRAINT delta_breaking_edge_pkey PRIMARY KEY (id )
);

CREATE TABLE delta_configuration_delta_breaking_edge
(
  id integer NOT NULL DEFAULT nextval('delta_configuration_delta_breaking_edge_seq'::regclass),
  delta_configuration_reference integer,
  delta_breaking_edge integer NOT NULL,
  CONSTRAINT delta_configuration_delta_breaking_edge_pkey PRIMARY KEY (id )
);

--------------------------------
-- rainevent
--------------------------------

CREATE SEQUENCE rainevent_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.rainevent_seq OWNER TO postgres;

--
-- TOC entry 283 (class 1259 OID 26684)
-- Dependencies: 3279 3280 6
-- Name: rainevent; Type: TABLE; Schema: public; Owner: postgres; Tablespace: 
--

CREATE TABLE rainevent (
    data text,
    name character varying(200) NOT NULL,
    id integer DEFAULT nextval('rainevent_seq'::regclass) NOT NULL,
    geom integer,
    forecast boolean DEFAULT false NOT NULL,
    description text,
    "interval" integer
);


ALTER TABLE public.rainevent OWNER TO postgres;