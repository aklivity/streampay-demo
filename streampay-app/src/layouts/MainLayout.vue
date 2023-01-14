<template>
  <q-layout view="lHh Lpr lFf" style="min-width: 400px">
    <q-drawer
      show-if-above
      :width="300"
      :breakpoint="500"
      bordered
    >
      <div class="absolute-top"  style="height: 150px; margin-top: 15px;">
        <div class="absolute-top">
          <div class="text-weight-bold text-h3 text-primary" style="margin-left: 10px;">StreamPay</div>
        </div>
        <div class="absolute-bottom" style="margin-top: 20px">
          <q-avatar size="60px" class="q-mb-sm" style="margin-left: 10px">
            <img :src="user.picture">
          </q-avatar>
          <div class="text-weight-bold float-right text-h6" style="padding-right: 10px; width: 222px; margin-top: 10px;">
            Hi, {{ user.name }}
          </div>
        </div>
      </div>

      <div style="margin-top: 200px; padding-left: 20px; padding-right: 20px;">
        <q-btn
          unelevated
          size="lg"
          color="primary"
          class="full-width text-white"
          label="Pay or Request"
          rounded
          @click="this.$router.push({ path: '/payorrequest' })"
        />
      </div>

      <div style="margin-top: 40px; padding-left: 20px; padding-right: 20px;">
        <div class="text-h6">
          <b>${{ balance }}</b> in StreamPay
        </div>
      </div>

      <q-list class="text-h6" style="margin-top: 20px;">
        <q-item
          clickable
          v-ripple
          @click="this.$router.push({ path: '/request' })"
        >
          <q-item-section avatar>
            <q-icon size="36px" color="primary" name="request_quote" />
          </q-item-section>

          <q-item-section>
              <div>Requests <q-badge rounded color="red" :label="requests" /></div>
          </q-item-section>
        </q-item>

        <!--
        <q-item clickable v-ripple>
          <q-item-section avatar>
            <q-icon size="36px" color="primary" name="analytics" />
          </q-item-section>

          <q-item-section>Statement</q-item-section>
        </q-item>
        -->
      </q-list>

      <div class="absolute-bottom text-weight-bold" style="padding-left: 80px; padding-right: 80px; margin-bottom: 30px;">
        <q-btn
          size="10px"
          color="negative"
          class="full-width text-white"
          label="Logout"
          rounded
          @click="logout"
        />
      </div>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue';
import {useAuth0} from '@auth0/auth0-vue';
import jwt_decode from 'jwt-decode'
import {api, streamingUrl} from "boot/axios";
import {Buffer} from "buffer";

export default defineComponent({
  name: 'MainLayout',

  setup () {
    const auth0 = useAuth0();
    const balance = ref(0);
    const requests = ref(0);

    api.get('/balances/user1')
      .then((response) => {
        balance.value = response.data.balance;
      })
      .catch(() => {
        balance.value = 0;
      });

    const requestStream = new EventSource(streamingUrl + "/payment-requests");

    requestStream.addEventListener('delete', (event: MessageEvent) => {
      let currentRequests = requests.value;
      currentRequests--;
      requests.value = currentRequests < 0 ? 0 : currentRequests;
    }, false);

    requestStream.onmessage = function (event: MessageEvent) {
      requests.value++;
    };

    return {
      auth0,
      isAuthenticated: auth0.isAuthenticated,
      isLoading: auth0.isLoading,
      user: auth0.user,
      balance,
      requests
    }
  },
  methods: {
     logout() {
      this.auth0.logout({
        returnTo: window.location.origin
      });
    },
    login() {
      this.auth0.loginWithRedirect();
    }
  }
});
</script>
