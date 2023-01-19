<template>
  <q-page class="items-center" style="margin-left: 12%; margin-right: 12%; margin-top: 70px;">
    <div class="items-center text-primary text-h4" style="margin-left: 40%; margin-bottom: 20px;">
      Activities
    </div>
    <q-table
      ref="tableRef"
      title-class="feed-title"
      hide-bottom
      hide-header
      card-style="box-shadow: none;"
      :rows="activities"
      :columns="columns"
      :table-colspan="9"
      row-key="index"
      virtual-scroll
      :virtual-scroll-item-size="48"
      :rows-per-page-options="[0]"
    >
       <template v-slot:body="props">
         <q-tr :props="props" no-hover>
           <q-td  key="avatar" :props="props" style="width: 50px">
             <q-avatar color="primary" text-color="white">{{ props.row.avatar }}</q-avatar>
           </q-td>
           <q-td  key="activities" :props="props">
             <div class="text-h6">
               <b>{{ props.row.from}}</b> {{ props.row.state }} <b>{{ props.row.to}}</b>
             </div>
             <div class="text-subtitle2">
               {{ props.row.date }}
             </div>
           </q-td>

           <q-td
             key="amount"
             :props="props"
           >
             <div class="text-subtitle1" :style="props.row.amount < 0 ? `color:red;` : 'color:green;'" >
               {{ props.row.amount }}
             </div>
           </q-td>
         </q-tr>
       </template>
    </q-table>
  </q-page>
</template>

<script lang="ts">
import {defineComponent, ref} from 'vue';
import {useAuth0} from "@auth0/auth0-vue";
import {streamingUrl} from "boot/axios";

export default defineComponent({
  name: 'MainPage',
  setup () {
    const auth0 = useAuth0();

    const tableRef = ref(null);

    const columns = [
      { name: 'avatar', align: 'left', field: 'avatar'},
      {
        name: 'activities',
        required: true,
        align: 'left',
        field: 'activities',
        format: (val: any) => `${val}`
      },
      { name: 'amount', align: 'right', field: 'amount'},
    ]

    const activities = ref([] as any);

    return {
      color: "text-red",
      auth0,
      user: auth0.user,
      tableRef,
      columns,
      activities,
      pagination: {
        rowsPerPage: 0
      }
    }
  },
  async mounted() {
    const accessToken = await this.auth0.getAccessTokenSilently();
    const userId = this.user.sub;
    const activitiesStream = new EventSource(`${streamingUrl}/activities?access_token=${accessToken}`);
    const activities = this.activities;

    activitiesStream.onopen = function () {
      activities.splice(0);
    }

    activitiesStream.onmessage = function (event: MessageEvent) {
      const activity = JSON.parse(event.data);
      if (activity.eventName == 'PaymentReceived' && activity.fromUserId == userId ||
        activity.eventName == 'PaymentSent' && activity.toUserId == userId) {

      } else {
        let state = '';

        if (activity.eventName == 'PaymentSent') {
          state = 'paid';
        } else if (activity.eventName == 'PaymentReceived') {
          state = 'paid';
        } else if (activity.eventName == 'PaymentRequested') {
          state = 'requested';
        }
        const from = activity.fromUserId == userId ? 'You' : activity.fromUserName;
        const to = activity.toUserId == userId ? 'you' : activity.toUserName;

        const avatar = from.charAt(0).toUpperCase();

        activities.push({
          avatar,
          from,
          to,
          state,
          amount: activity.amount,
          date: new Date (activity.timestamp)
        });
      }
    };
  }
});
</script>
